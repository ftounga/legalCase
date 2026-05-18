package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * F-98 / SF-98-01 + SF-98-52 — tests unitaires du déclenchement de génération de
 * conclusions versionnées : nominal versionné (v1 → v2 → v3), les 4 gardes 409,
 * isolation workspace, transitions de cycle de vie + gardes 409/400.
 */
class CaseConclusionCommandServiceTest {

    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final CaseConclusionRepository caseConclusionRepository = mock(CaseConclusionRepository.class);
    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    private final CaseConclusionCommandService service = new CaseConclusionCommandService(
            caseFileRepository, caseConclusionRepository, caseAnalysisRepository,
            currentUserResolver, workspaceMemberRepository, rabbitTemplate);

    // ── génération versionnée (CA2, CA9) ─────────────────────────────────────

    @Test
    void triggerGeneration_firstVersion_createsVersionOneDraftPending() {
        Ctx ctx = supportedCase();
        when(caseConclusionRepository.findFirstByCaseFileIdOrderByVersionNumberDesc(ctx.caseFileId))
                .thenReturn(Optional.empty());
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                ctx.caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(new CaseAnalysis()));
        when(caseConclusionRepository.existsByCaseFileIdAndStatusIn(eq(ctx.caseFileId), any()))
                .thenReturn(false);
        when(caseConclusionRepository.save(any())).thenAnswer(inv -> {
            CaseConclusion c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
            }
            return c;
        });

        ConclusionGenerationResponse response = service.triggerGeneration(ctx.caseFileId, null, null, null);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.versionNumber()).isEqualTo(1);
        var captor = org.mockito.ArgumentCaptor.forClass(CaseConclusion.class);
        verify(caseConclusionRepository).save(captor.capture());
        CaseConclusion saved = captor.getValue();
        assertThat(saved.getVersionNumber()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo(CaseConclusionStatus.PENDING);
        assertThat(saved.getLifecycleStatus()).isEqualTo(ConclusionLifecycleStatus.DRAFT);
        assertThat(saved.getJurisdictionCode()).isEqualTo("CPH");
        assertThat(saved.getStageCode()).isEqualTo("FOND");
        assertThat(saved.getPositionCode()).isEqualTo("DEMANDEUR");
        verify(rabbitTemplate).convertAndSend(
                eq(CaseConclusionRabbitMQConfig.CASE_CONCLUSION_EXCHANGE),
                eq(CaseConclusionRabbitMQConfig.CASE_CONCLUSION_ROUTING_KEY),
                any(CaseConclusionMessage.class));
    }

    @Test
    void triggerGeneration_existingVersions_createsNextVersionWithoutTouchingPrevious() {
        Ctx ctx = supportedCase();
        CaseConclusion v2 = new CaseConclusion();
        v2.setId(UUID.randomUUID());
        v2.setVersionNumber(2);
        v2.setStatus(CaseConclusionStatus.DONE);
        v2.setContent("texte v2");
        when(caseConclusionRepository.findFirstByCaseFileIdOrderByVersionNumberDesc(ctx.caseFileId))
                .thenReturn(Optional.of(v2));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                ctx.caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(new CaseAnalysis()));
        when(caseConclusionRepository.existsByCaseFileIdAndStatusIn(eq(ctx.caseFileId), any()))
                .thenReturn(false);
        when(caseConclusionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConclusionGenerationResponse response = service.triggerGeneration(ctx.caseFileId, null, null, null);

        assertThat(response.versionNumber()).isEqualTo(3);
        var captor = org.mockito.ArgumentCaptor.forClass(CaseConclusion.class);
        verify(caseConclusionRepository).save(captor.capture());
        CaseConclusion saved = captor.getValue();
        assertThat(saved.getVersionNumber()).isEqualTo(3);
        assertThat(saved.getStatus()).isEqualTo(CaseConclusionStatus.PENDING);
        assertThat(saved.getLifecycleStatus()).isEqualTo(ConclusionLifecycleStatus.DRAFT);
        // version précédente intacte
        assertThat(v2.getVersionNumber()).isEqualTo(2);
        assertThat(v2.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        assertThat(v2.getContent()).isEqualTo("texte v2");
    }

    // ── gardes 409 ───────────────────────────────────────────────────────────

    @Test
    void triggerGeneration_stageIncomplete_throwsStageNotSet() {
        Ctx ctx = supportedCase();
        ctx.caseFile.setProcedurePosition(null);

        assertThatThrownBy(() -> service.triggerGeneration(ctx.caseFileId, null, null, null))
                .isInstanceOf(CaseConclusionGuardException.class)
                .extracting(e -> ((CaseConclusionGuardException) e).getCode())
                .isEqualTo(CaseConclusionGuardCode.STAGE_NOT_SET);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void triggerGeneration_unsupportedCombination_throwsCombinationNotSupported() {
        Ctx ctx = supportedCase();
        ctx.caseFile.setProcedureStage("REFERE"); // hors FOND

        assertThatThrownBy(() -> service.triggerGeneration(ctx.caseFileId, null, null, null))
                .isInstanceOf(CaseConclusionGuardException.class)
                .extracting(e -> ((CaseConclusionGuardException) e).getCode())
                .isEqualTo(CaseConclusionGuardCode.COMBINATION_NOT_SUPPORTED);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void triggerGeneration_unsupportedCountry_throwsCombinationNotSupported() {
        Ctx ctx = supportedCase();
        ctx.workspace.setCountry("BELGIQUE");

        assertThatThrownBy(() -> service.triggerGeneration(ctx.caseFileId, null, null, null))
                .isInstanceOf(CaseConclusionGuardException.class)
                .extracting(e -> ((CaseConclusionGuardException) e).getCode())
                .isEqualTo(CaseConclusionGuardCode.COMBINATION_NOT_SUPPORTED);
    }

    @Test
    void triggerGeneration_noDoneAnalysis_throwsAnalysisNotReady() {
        Ctx ctx = supportedCase();
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                ctx.caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.triggerGeneration(ctx.caseFileId, null, null, null))
                .isInstanceOf(CaseConclusionGuardException.class)
                .extracting(e -> ((CaseConclusionGuardException) e).getCode())
                .isEqualTo(CaseConclusionGuardCode.ANALYSIS_NOT_READY);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void triggerGeneration_versionAlreadyGenerating_throwsAlreadyGenerating() {
        Ctx ctx = supportedCase();
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                ctx.caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(new CaseAnalysis()));
        when(caseConclusionRepository.existsByCaseFileIdAndStatusIn(eq(ctx.caseFileId), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.triggerGeneration(ctx.caseFileId, null, null, null))
                .isInstanceOf(CaseConclusionGuardException.class)
                .extracting(e -> ((CaseConclusionGuardException) e).getCode())
                .isEqualTo(CaseConclusionGuardCode.ALREADY_GENERATING);
        verifyNoInteractions(rabbitTemplate);
    }

    // ── isolation workspace ──────────────────────────────────────────────────

    @Test
    void triggerGeneration_caseFileFromOtherWorkspace_throws404() {
        Ctx ctx = supportedCase();
        Workspace otherWs = new Workspace();
        otherWs.setId(UUID.randomUUID());
        otherWs.setCountry("FRANCE");
        ctx.caseFile.setWorkspace(otherWs); // dossier dans un autre workspace

        assertThatThrownBy(() -> service.triggerGeneration(ctx.caseFileId, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void getConclusion_noVersion_returnsNotGenerated() {
        Ctx ctx = supportedCase();
        when(caseConclusionRepository.findFirstByCaseFileIdOrderByVersionNumberDesc(ctx.caseFileId))
                .thenReturn(Optional.empty());

        ConclusionResponse response = service.getConclusion(ctx.caseFileId, null, null, null);

        assertThat(response.status()).isEqualTo(ConclusionResponse.NOT_GENERATED);
        assertThat(response.caseFileId()).isEqualTo(ctx.caseFileId);
        assertThat(response.content()).isNull();
        assertThat(response.versionNumber()).isZero();
    }

    @Test
    void getConclusion_returnsMostRecentVersion() {
        Ctx ctx = supportedCase();
        CaseConclusion v3 = doneVersion(ctx, 3);
        when(caseConclusionRepository.findFirstByCaseFileIdOrderByVersionNumberDesc(ctx.caseFileId))
                .thenReturn(Optional.of(v3));

        ConclusionResponse response = service.getConclusion(ctx.caseFileId, null, null, null);

        assertThat(response.versionNumber()).isEqualTo(3);
        assertThat(response.status()).isEqualTo("DONE");
        assertThat(response.lifecycleStatus()).isEqualTo("DRAFT");
    }

    // ── SF-98-53 — péremption (stale) sur getConclusion / getVersion ─────────

    @Test
    void getConclusion_analysisUpdatedAfterGeneration_staleTrue() {
        Ctx ctx = supportedCase();
        Instant generatedAt = Instant.parse("2026-05-10T10:00:00Z");
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setGeneratedAt(generatedAt);
        when(caseConclusionRepository.findFirstByCaseFileIdOrderByVersionNumberDesc(ctx.caseFileId))
                .thenReturn(Optional.of(v1));
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setUpdatedAt(generatedAt.plusSeconds(3600)); // analyse postérieure
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                ctx.caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(analysis));

        ConclusionResponse response = service.getConclusion(ctx.caseFileId, null, null, null);

        assertThat(response.stale()).isTrue();
    }

    @Test
    void getConclusion_analysisUpdatedBeforeGeneration_staleFalse() {
        Ctx ctx = supportedCase();
        Instant generatedAt = Instant.parse("2026-05-10T10:00:00Z");
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setGeneratedAt(generatedAt);
        when(caseConclusionRepository.findFirstByCaseFileIdOrderByVersionNumberDesc(ctx.caseFileId))
                .thenReturn(Optional.of(v1));
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setUpdatedAt(generatedAt.minusSeconds(3600)); // analyse antérieure
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                ctx.caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(analysis));

        ConclusionResponse response = service.getConclusion(ctx.caseFileId, null, null, null);

        assertThat(response.stale()).isFalse();
    }

    @Test
    void getConclusion_noDoneAnalysis_staleFalse() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setGeneratedAt(Instant.parse("2026-05-10T10:00:00Z"));
        when(caseConclusionRepository.findFirstByCaseFileIdOrderByVersionNumberDesc(ctx.caseFileId))
                .thenReturn(Optional.of(v1));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                ctx.caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.empty());

        ConclusionResponse response = service.getConclusion(ctx.caseFileId, null, null, null);

        assertThat(response.stale()).isFalse();
    }

    @Test
    void getConclusion_generatedAtNull_staleFalse() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setGeneratedAt(null);
        when(caseConclusionRepository.findFirstByCaseFileIdOrderByVersionNumberDesc(ctx.caseFileId))
                .thenReturn(Optional.of(v1));

        ConclusionResponse response = service.getConclusion(ctx.caseFileId, null, null, null);

        assertThat(response.stale()).isFalse();
        // pas DONE/generatedAt → on ne touche pas le dépôt d'analyses
        verify(caseAnalysisRepository, never())
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any());
    }

    @Test
    void getConclusion_versionNotDone_staleFalse() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setStatus(CaseConclusionStatus.PENDING); // pas DONE
        v1.setGeneratedAt(Instant.parse("2026-05-10T10:00:00Z"));
        when(caseConclusionRepository.findFirstByCaseFileIdOrderByVersionNumberDesc(ctx.caseFileId))
                .thenReturn(Optional.of(v1));

        ConclusionResponse response = service.getConclusion(ctx.caseFileId, null, null, null);

        assertThat(response.stale()).isFalse();
        verify(caseAnalysisRepository, never())
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any());
    }

    @Test
    void getConclusion_notGenerated_staleFalse() {
        Ctx ctx = supportedCase();
        when(caseConclusionRepository.findFirstByCaseFileIdOrderByVersionNumberDesc(ctx.caseFileId))
                .thenReturn(Optional.empty());

        ConclusionResponse response = service.getConclusion(ctx.caseFileId, null, null, null);

        assertThat(response.stale()).isFalse();
    }

    @Test
    void getConclusion_staleComputationThrows_failOpenStaleFalse() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setGeneratedAt(Instant.parse("2026-05-10T10:00:00Z"));
        when(caseConclusionRepository.findFirstByCaseFileIdOrderByVersionNumberDesc(ctx.caseFileId))
                .thenReturn(Optional.of(v1));
        // le calcul de péremption échoue → fail-open
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                ctx.caseFileId, AnalysisStatus.DONE))
                .thenThrow(new RuntimeException("DB down"));

        ConclusionResponse response = service.getConclusion(ctx.caseFileId, null, null, null);

        assertThat(response.stale()).isFalse();
        assertThat(response.versionNumber()).isEqualTo(1);
    }

    @Test
    void getVersion_analysisUpdatedAfterGeneration_staleTrue() {
        Ctx ctx = supportedCase();
        Instant generatedAt = Instant.parse("2026-05-10T10:00:00Z");
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setGeneratedAt(generatedAt);
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setUpdatedAt(generatedAt.plusSeconds(3600));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                ctx.caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(analysis));

        ConclusionResponse response = service.getVersion(ctx.caseFileId, v1.getId(), null, null, null);

        assertThat(response.stale()).isTrue();
    }

    @Test
    void getVersion_analysisUpdatedBeforeGeneration_staleFalse() {
        Ctx ctx = supportedCase();
        Instant generatedAt = Instant.parse("2026-05-10T10:00:00Z");
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setGeneratedAt(generatedAt);
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setUpdatedAt(generatedAt.minusSeconds(3600));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                ctx.caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(analysis));

        ConclusionResponse response = service.getVersion(ctx.caseFileId, v1.getId(), null, null, null);

        assertThat(response.stale()).isFalse();
    }

    // ── listVersions (CA4) ───────────────────────────────────────────────────

    @Test
    void listVersions_returnsSummariesVersionDescending() {
        Ctx ctx = supportedCase();
        when(caseConclusionRepository.findByCaseFileIdOrderByVersionNumberDesc(ctx.caseFileId))
                .thenReturn(List.of(doneVersion(ctx, 2), doneVersion(ctx, 1)));

        List<ConclusionVersionSummary> versions = service.listVersions(ctx.caseFileId, null, null, null);

        assertThat(versions).hasSize(2);
        assertThat(versions.get(0).versionNumber()).isEqualTo(2);
        assertThat(versions.get(1).versionNumber()).isEqualTo(1);
        assertThat(versions.get(0).lifecycleStatus()).isEqualTo("DRAFT");
    }

    // ── getVersion (CA5) ─────────────────────────────────────────────────────

    @Test
    void getVersion_unknownVersion_throws404() {
        Ctx ctx = supportedCase();
        UUID versionId = UUID.randomUUID();
        when(caseConclusionRepository.findByIdAndCaseFileId(versionId, ctx.caseFileId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVersion(ctx.caseFileId, versionId, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // ── updateLifecycle (CA6) ────────────────────────────────────────────────

    @Test
    void updateLifecycle_doneVersionToValidated_succeeds() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));
        when(caseConclusionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConclusionResponse response = service.updateLifecycle(
                ctx.caseFileId, v1.getId(), "VALIDATED", null, null, null);

        assertThat(response.lifecycleStatus()).isEqualTo("VALIDATED");
        assertThat(v1.getLifecycleStatus()).isEqualTo(ConclusionLifecycleStatus.VALIDATED);
    }

    @Test
    void updateLifecycle_validatedBackToDraft_succeeds() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setLifecycleStatus(ConclusionLifecycleStatus.VALIDATED);
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));
        when(caseConclusionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConclusionResponse response = service.updateLifecycle(
                ctx.caseFileId, v1.getId(), "DRAFT", null, null, null);

        assertThat(response.lifecycleStatus()).isEqualTo("DRAFT");
    }

    @Test
    void updateLifecycle_nonDoneVersionToValidated_throws409() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setStatus(CaseConclusionStatus.PENDING); // pas DONE
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));

        assertThatThrownBy(() -> service.updateLifecycle(
                ctx.caseFileId, v1.getId(), "VALIDATED", null, null, null))
                .isInstanceOf(CaseConclusionGuardException.class)
                .extracting(e -> ((CaseConclusionGuardException) e).getCode())
                .isEqualTo(CaseConclusionGuardCode.LIFECYCLE_REQUIRES_DONE);
        verify(caseConclusionRepository, never()).save(any());
    }

    @Test
    void updateLifecycle_nonDoneVersionToDeposited_throws409() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setStatus(CaseConclusionStatus.FAILED); // pas DONE
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));

        assertThatThrownBy(() -> service.updateLifecycle(
                ctx.caseFileId, v1.getId(), "DEPOSITED", null, null, null))
                .isInstanceOf(CaseConclusionGuardException.class)
                .extracting(e -> ((CaseConclusionGuardException) e).getCode())
                .isEqualTo(CaseConclusionGuardCode.LIFECYCLE_REQUIRES_DONE);
    }

    @Test
    void updateLifecycle_nonDoneVersionToDraft_succeeds() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setStatus(CaseConclusionStatus.PENDING);
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));
        when(caseConclusionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConclusionResponse response = service.updateLifecycle(
                ctx.caseFileId, v1.getId(), "DRAFT", null, null, null);

        assertThat(response.lifecycleStatus()).isEqualTo("DRAFT");
    }

    @Test
    void updateLifecycle_unknownValue_throws400() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));

        assertThatThrownBy(() -> service.updateLifecycle(
                ctx.caseFileId, v1.getId(), "ARCHIVED", null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
        verify(caseConclusionRepository, never()).save(any());
    }

    @Test
    void updateLifecycle_nullValue_throws400() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));

        assertThatThrownBy(() -> service.updateLifecycle(
                ctx.caseFileId, v1.getId(), null, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    // ── updateContent (SF-98-49) ─────────────────────────────────────────────

    @Test
    void updateContent_doneDraftVersion_updatesContentAndReturnsResponse() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));
        when(caseConclusionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConclusionResponse response = service.updateContent(
                ctx.caseFileId, v1.getId(), "Texte révisé par l'avocat", null, null, null);

        assertThat(response.content()).isEqualTo("Texte révisé par l'avocat");
        assertThat(v1.getContent()).isEqualTo("Texte révisé par l'avocat");
        verify(caseConclusionRepository).save(v1);
    }

    @Test
    void updateContent_nonDoneVersion_throws409ContentRequiresDone() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setStatus(CaseConclusionStatus.PENDING); // pas DONE
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));

        assertThatThrownBy(() -> service.updateContent(
                ctx.caseFileId, v1.getId(), "Texte révisé", null, null, null))
                .isInstanceOf(CaseConclusionGuardException.class)
                .extracting(e -> ((CaseConclusionGuardException) e).getCode())
                .isEqualTo(CaseConclusionGuardCode.CONTENT_REQUIRES_DONE);
        verify(caseConclusionRepository, never()).save(any());
    }

    @Test
    void updateContent_validatedVersion_throws409ContentNotEditable() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setLifecycleStatus(ConclusionLifecycleStatus.VALIDATED);
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));

        assertThatThrownBy(() -> service.updateContent(
                ctx.caseFileId, v1.getId(), "Texte révisé", null, null, null))
                .isInstanceOf(CaseConclusionGuardException.class)
                .extracting(e -> ((CaseConclusionGuardException) e).getCode())
                .isEqualTo(CaseConclusionGuardCode.CONTENT_NOT_EDITABLE);
        verify(caseConclusionRepository, never()).save(any());
    }

    @Test
    void updateContent_depositedVersion_throws409ContentNotEditable() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        v1.setLifecycleStatus(ConclusionLifecycleStatus.DEPOSITED);
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));

        assertThatThrownBy(() -> service.updateContent(
                ctx.caseFileId, v1.getId(), "Texte révisé", null, null, null))
                .isInstanceOf(CaseConclusionGuardException.class)
                .extracting(e -> ((CaseConclusionGuardException) e).getCode())
                .isEqualTo(CaseConclusionGuardCode.CONTENT_NOT_EDITABLE);
        verify(caseConclusionRepository, never()).save(any());
    }

    @Test
    void updateContent_blankContent_throws400() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));

        assertThatThrownBy(() -> service.updateContent(
                ctx.caseFileId, v1.getId(), "   ", null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
        verify(caseConclusionRepository, never()).save(any());
    }

    @Test
    void updateContent_nullContent_throws400() {
        Ctx ctx = supportedCase();
        CaseConclusion v1 = doneVersion(ctx, 1);
        when(caseConclusionRepository.findByIdAndCaseFileId(v1.getId(), ctx.caseFileId))
                .thenReturn(Optional.of(v1));

        assertThatThrownBy(() -> service.updateContent(
                ctx.caseFileId, v1.getId(), null, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
        verify(caseConclusionRepository, never()).save(any());
    }

    @Test
    void updateContent_unknownVersion_throws404() {
        Ctx ctx = supportedCase();
        UUID versionId = UUID.randomUUID();
        when(caseConclusionRepository.findByIdAndCaseFileId(versionId, ctx.caseFileId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateContent(
                ctx.caseFileId, versionId, "Texte révisé", null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private record Ctx(UUID caseFileId, CaseFile caseFile, Workspace workspace) {
    }

    /** Construit un dossier travail FR / CPH / FOND / DEMANDEUR valide pour la V1. */
    private Ctx supportedCase() {
        User user = new User();
        user.setId(UUID.randomUUID());

        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setCountry("FRANCE");

        CaseFile caseFile = new CaseFile();
        UUID caseFileId = UUID.randomUUID();
        caseFile.setId(caseFileId);
        caseFile.setWorkspace(workspace);
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setProcedureJurisdiction("CPH");
        caseFile.setProcedureStage("FOND");
        caseFile.setProcedurePosition("DEMANDEUR");

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));

        return new Ctx(caseFileId, caseFile, workspace);
    }

    /** Version DONE / DRAFT au numéro donné, rattachée au dossier du contexte. */
    private CaseConclusion doneVersion(Ctx ctx, int versionNumber) {
        CaseConclusion c = new CaseConclusion();
        c.setId(UUID.randomUUID());
        c.setCaseFile(ctx.caseFile);
        c.setWorkspace(ctx.workspace);
        c.setVersionNumber(versionNumber);
        c.setStatus(CaseConclusionStatus.DONE);
        c.setLifecycleStatus(ConclusionLifecycleStatus.DRAFT);
        c.setJurisdictionCode("CPH");
        c.setStageCode("FOND");
        c.setPositionCode("DEMANDEUR");
        c.setContent("texte v" + versionNumber);
        return c;
    }
}
