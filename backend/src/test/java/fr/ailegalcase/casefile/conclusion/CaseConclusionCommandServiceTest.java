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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * F-98 / SF-98-01 — tests unitaires du déclenchement de génération de conclusions :
 * nominal + les 4 gardes 409 + isolation workspace.
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

    // ── nominal ──────────────────────────────────────────────────────────────

    @Test
    void triggerGeneration_nominal_createsPendingRowAndPublishesMessage() {
        Ctx ctx = supportedCase();
        when(caseConclusionRepository.findByCaseFileId(ctx.caseFileId)).thenReturn(Optional.empty());
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                ctx.caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(new CaseAnalysis()));
        when(caseConclusionRepository.save(any())).thenAnswer(inv -> {
            CaseConclusion c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
            }
            return c;
        });

        ConclusionGenerationResponse response = service.triggerGeneration(ctx.caseFileId, null, null, null);

        assertThat(response.status()).isEqualTo("PENDING");
        var captor = org.mockito.ArgumentCaptor.forClass(CaseConclusion.class);
        verify(caseConclusionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CaseConclusionStatus.PENDING);
        assertThat(captor.getValue().getJurisdictionCode()).isEqualTo("CPH");
        assertThat(captor.getValue().getStageCode()).isEqualTo("FOND");
        assertThat(captor.getValue().getPositionCode()).isEqualTo("DEMANDEUR");
        verify(rabbitTemplate).convertAndSend(
                eq(CaseConclusionRabbitMQConfig.CASE_CONCLUSION_EXCHANGE),
                eq(CaseConclusionRabbitMQConfig.CASE_CONCLUSION_ROUTING_KEY),
                any(CaseConclusionMessage.class));
    }

    @Test
    void triggerGeneration_regenerate_reusesRowAndPurgesPreviousResult() {
        Ctx ctx = supportedCase();
        CaseConclusion existing = new CaseConclusion();
        existing.setId(UUID.randomUUID());
        existing.setStatus(CaseConclusionStatus.DONE);
        existing.setContent("ancien texte");
        existing.setErrorMessage(null);
        when(caseConclusionRepository.findByCaseFileId(ctx.caseFileId)).thenReturn(Optional.of(existing));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                ctx.caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(new CaseAnalysis()));
        when(caseConclusionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.triggerGeneration(ctx.caseFileId, null, null, null);

        assertThat(existing.getStatus()).isEqualTo(CaseConclusionStatus.PENDING);
        assertThat(existing.getContent()).isNull();
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(CaseConclusionMessage.class));
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
        when(caseConclusionRepository.findByCaseFileId(ctx.caseFileId)).thenReturn(Optional.empty());
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                ctx.caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.triggerGeneration(ctx.caseFileId, null, null, null))
                .isInstanceOf(CaseConclusionGuardException.class)
                .extracting(e -> ((CaseConclusionGuardException) e).getCode())
                .isEqualTo(CaseConclusionGuardCode.ANALYSIS_NOT_READY);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void triggerGeneration_generationAlreadyRunning_throwsAlreadyGenerating() {
        Ctx ctx = supportedCase();
        CaseConclusion processing = new CaseConclusion();
        processing.setId(UUID.randomUUID());
        processing.setStatus(CaseConclusionStatus.PROCESSING);
        when(caseConclusionRepository.findByCaseFileId(ctx.caseFileId)).thenReturn(Optional.of(processing));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                ctx.caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(new CaseAnalysis()));

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
    void getConclusion_noRow_returnsNotGenerated() {
        Ctx ctx = supportedCase();
        when(caseConclusionRepository.findByCaseFileId(ctx.caseFileId)).thenReturn(Optional.empty());

        ConclusionResponse response = service.getConclusion(ctx.caseFileId, null, null, null);

        assertThat(response.status()).isEqualTo(ConclusionResponse.NOT_GENERATED);
        assertThat(response.caseFileId()).isEqualTo(ctx.caseFileId);
        assertThat(response.content()).isNull();
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
}
