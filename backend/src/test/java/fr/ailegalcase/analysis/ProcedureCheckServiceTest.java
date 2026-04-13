package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;

class ProcedureCheckServiceTest {

    private final ProcedureCheckRepository procedureCheckRepository = mock(ProcedureCheckRepository.class);
    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final ProcedureCheckService service = new ProcedureCheckService(
            procedureCheckRepository, caseAnalysisRepository, caseFileRepository,
            workspaceMemberRepository, currentUserResolver, eventPublisher);

    // ---- createChecks ----

    @Test
    void createChecks_objectFormatWithCritereCode_persistsCodeUpperCase() {
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(new Workspace());
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);

        String json = """
                {"points_procedure": [
                  {"texte": "Convocation par LRAR", "critere_code": "FR_CONVOCATION"},
                  {"texte": "Motivation précise", "critere_code": "fr_motivation"},
                  {"texte": "Point sans code"}
                ]}
                """;

        service.createChecks(analysis, json);

        ArgumentCaptor<ProcedureCheck> captor = ArgumentCaptor.forClass(ProcedureCheck.class);
        verify(procedureCheckRepository, times(3)).save(captor.capture());
        List<ProcedureCheck> saved = captor.getAllValues();

        assertThat(saved.get(0).getCritereCode()).isEqualTo("FR_CONVOCATION");
        assertThat(saved.get(0).getDescription()).isEqualTo("Convocation par LRAR");
        assertThat(saved.get(1).getCritereCode()).isEqualTo("FR_MOTIVATION");
        assertThat(saved.get(2).getCritereCode()).isNull();
    }

    @Test
    void createChecks_mixedLegacyAndObjectFormats_parsesBoth() {
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(new Workspace());
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);

        String json = """
                {"points_procedure": [
                  "Point legacy string",
                  {"texte": "Point objet avec code", "critere_code": "FR_ENTRETIEN"}
                ]}
                """;

        service.createChecks(analysis, json);

        ArgumentCaptor<ProcedureCheck> captor = ArgumentCaptor.forClass(ProcedureCheck.class);
        verify(procedureCheckRepository, times(2)).save(captor.capture());
        List<ProcedureCheck> saved = captor.getAllValues();
        assertThat(saved.get(0).getDescription()).isEqualTo("Point legacy string");
        assertThat(saved.get(0).getCritereCode()).isNull();
        assertThat(saved.get(1).getDescription()).isEqualTo("Point objet avec code");
        assertThat(saved.get(1).getCritereCode()).isEqualTo("FR_ENTRETIEN");
    }

    @Test
    void createChecks_critereCodeBlankOrMissingTexte_isIgnoredOrNull() {
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(new Workspace());
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);

        String json = """
                {"points_procedure": [
                  {"texte": "Point valide", "critere_code": "   "},
                  {"texte": "", "critere_code": "FR_MOTIVATION"},
                  {"critere_code": "FR_CONVOCATION"}
                ]}
                """;

        service.createChecks(analysis, json);

        ArgumentCaptor<ProcedureCheck> captor = ArgumentCaptor.forClass(ProcedureCheck.class);
        verify(procedureCheckRepository, times(1)).save(captor.capture());
        // Seul le premier point est gardé (texte non vide), code blank → null
        assertThat(captor.getValue().getDescription()).isEqualTo("Point valide");
        assertThat(captor.getValue().getCritereCode()).isNull();
    }

    @Test
    void createChecks_nominalList_createsChecksWithCorrectOrdreAndDescription() {
        CaseFile caseFile = new CaseFile();
        Workspace workspace = new Workspace();
        caseFile.setWorkspace(workspace);

        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);

        String json = """
                {"points_procedure": ["Entretien préalable tenu", "Lettre motivée envoyée", "Délai de réflexion respecté"]}
                """;

        service.createChecks(analysis, json);

        ArgumentCaptor<ProcedureCheck> captor = ArgumentCaptor.forClass(ProcedureCheck.class);
        verify(procedureCheckRepository, times(3)).save(captor.capture());
        List<ProcedureCheck> saved = captor.getAllValues();

        assertThat(saved).hasSize(3);
        assertThat(saved.get(0).getOrdre()).isEqualTo(0);
        assertThat(saved.get(0).getDescription()).isEqualTo("Entretien préalable tenu");
        assertThat(saved.get(0).getStatut()).isEqualTo(ProcedureCheckStatus.TO_CHECK);
        assertThat(saved.get(1).getOrdre()).isEqualTo(1);
        assertThat(saved.get(2).getOrdre()).isEqualTo(2);
        assertThat(saved.get(2).getDescription()).isEqualTo("Délai de réflexion respecté");
    }

    @Test
    void createChecks_pointsProcedureAbsent_createsNoChecks() {
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(new Workspace());
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);

        String json = """
                {"faits": ["fait1"], "risques": []}
                """;

        service.createChecks(analysis, json);

        verify(procedureCheckRepository, never()).save(any());
    }

    @Test
    void createChecks_emptyList_createsNoChecks() {
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(new Workspace());
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);

        service.createChecks(analysis, "{\"points_procedure\": []}");

        verify(procedureCheckRepository, never()).save(any());
    }

    @Test
    void createChecks_malformedJson_createsNoChecksNoException() {
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(new Workspace());
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);

        service.createChecks(analysis, "{ not valid json }");

        verify(procedureCheckRepository, never()).save(any());
    }

    @Test
    void createChecks_nullJson_createsNoChecks() {
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(new CaseFile());

        service.createChecks(analysis, null);

        verify(procedureCheckRepository, never()).save(any());
    }

    // ---- listVerified ----

    @Test
    void listVerified_returnsVerifiedDescriptions() {
        UUID caseFileId = UUID.randomUUID();
        UUID analysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);

        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setId(analysisId);

        ProcedureCheck check = new ProcedureCheck();
        check.setDescription("Entretien préalable tenu");
        check.setStatut(ProcedureCheckStatus.VERIFIED);

        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(analysis));
        when(procedureCheckRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(analysisId, ProcedureCheckStatus.VERIFIED))
                .thenReturn(List.of(check));

        List<String> result = service.listVerified(caseFile);

        assertThat(result).containsExactly("Entretien préalable tenu");
    }

    @Test
    void listVerified_noAnalysis_returnsEmpty() {
        CaseFile caseFile = new CaseFile();
        caseFile.setId(UUID.randomUUID());
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());

        assertThat(service.listVerified(caseFile)).isEmpty();
    }

    @Test
    void listVerified_repoThrows_returnsEmpty() {
        CaseFile caseFile = new CaseFile();
        caseFile.setId(UUID.randomUUID());
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        assertThat(service.listVerified(caseFile)).isEmpty();
    }

    // ---- createChecksWithVerifiedPropagation ----

    @Test
    void createChecksWithVerifiedPropagation_nullPreviousAnalysisId_onlyCreatesNewChecks() {
        UUID analysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        Workspace workspace = new Workspace();
        caseFile.setWorkspace(workspace);

        CaseAnalysis newAnalysis = new CaseAnalysis();
        newAnalysis.setId(analysisId);
        newAnalysis.setCaseFile(caseFile);

        String json = "{\"points_procedure\": [\"Entretien préalable tenu\"]}";

        service.createChecksWithVerifiedPropagation(newAnalysis, json, null);

        ArgumentCaptor<ProcedureCheck> captor = ArgumentCaptor.forClass(ProcedureCheck.class);
        verify(procedureCheckRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Entretien préalable tenu");
        assertThat(captor.getValue().getStatut()).isEqualTo(ProcedureCheckStatus.TO_CHECK);
    }

    @Test
    void createChecksWithVerifiedPropagation_propagatesVerifiedFromPreviousAnalysis() {
        UUID newAnalysisId = UUID.randomUUID();
        UUID prevAnalysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        Workspace workspace = new Workspace();
        caseFile.setWorkspace(workspace);

        CaseAnalysis newAnalysis = new CaseAnalysis();
        newAnalysis.setId(newAnalysisId);
        newAnalysis.setCaseFile(caseFile);

        ProcedureCheck verifiedCheck = new ProcedureCheck();
        verifiedCheck.setDescription("Convocation par LRAR envoyée");
        verifiedCheck.setStatut(ProcedureCheckStatus.VERIFIED);
        verifiedCheck.setOrdre(0);

        when(procedureCheckRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(prevAnalysisId, ProcedureCheckStatus.VERIFIED))
                .thenReturn(List.of(verifiedCheck));
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(newAnalysisId))
                .thenReturn(List.of());

        String json = "{\"points_procedure\": [], \"checks_a_requalifier\": []}";
        service.createChecksWithVerifiedPropagation(newAnalysis, json, prevAnalysisId);

        ArgumentCaptor<ProcedureCheck> captor = ArgumentCaptor.forClass(ProcedureCheck.class);
        verify(procedureCheckRepository, atLeastOnce()).save(captor.capture());

        List<ProcedureCheck> saved = captor.getAllValues();
        assertThat(saved).anyMatch(c -> c.getStatut() == ProcedureCheckStatus.VERIFIED
                && "Convocation par LRAR envoyée".equals(c.getDescription()));
    }

    @Test
    void createChecksWithVerifiedPropagation_requalifiesMatchingVerified() {
        UUID newAnalysisId = UUID.randomUUID();
        UUID prevAnalysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(new Workspace());

        CaseAnalysis newAnalysis = new CaseAnalysis();
        newAnalysis.setId(newAnalysisId);
        newAnalysis.setCaseFile(caseFile);

        ProcedureCheck verifiedCheck = new ProcedureCheck();
        verifiedCheck.setDescription("Entretien préalable tenu");
        verifiedCheck.setStatut(ProcedureCheckStatus.VERIFIED);

        when(procedureCheckRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(prevAnalysisId, ProcedureCheckStatus.VERIFIED))
                .thenReturn(List.of(verifiedCheck));
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(newAnalysisId))
                .thenReturn(List.of());
        when(procedureCheckRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String json = """
                {"points_procedure": [], "checks_a_requalifier": [
                    {"description": "Entretien préalable tenu", "nouveau_statut": "NON_COMPLIANT", "raison": "Nouveau document contredit ce point"}
                ]}""";
        service.createChecksWithVerifiedPropagation(newAnalysis, json, prevAnalysisId);

        assertThat(verifiedCheck.getStatut()).isEqualTo(ProcedureCheckStatus.NON_COMPLIANT);
        assertThat(verifiedCheck.getRaison()).isEqualTo("Nouveau document contredit ce point");
    }

    @Test
    void createChecksWithVerifiedPropagation_invalidNewStatut_ignoredFailOpen() {
        UUID newAnalysisId = UUID.randomUUID();
        UUID prevAnalysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(new Workspace());

        CaseAnalysis newAnalysis = new CaseAnalysis();
        newAnalysis.setId(newAnalysisId);
        newAnalysis.setCaseFile(caseFile);

        ProcedureCheck verifiedCheck = new ProcedureCheck();
        verifiedCheck.setDescription("Entretien préalable tenu");
        verifiedCheck.setStatut(ProcedureCheckStatus.VERIFIED);

        when(procedureCheckRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(prevAnalysisId, ProcedureCheckStatus.VERIFIED))
                .thenReturn(List.of(verifiedCheck));
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(newAnalysisId))
                .thenReturn(List.of());

        String json = """
                {"points_procedure": [], "checks_a_requalifier": [
                    {"description": "Entretien préalable tenu", "nouveau_statut": "STATUT_INVALIDE", "raison": "raison"}
                ]}""";
        service.createChecksWithVerifiedPropagation(newAnalysis, json, prevAnalysisId);

        // statut non modifié
        assertThat(verifiedCheck.getStatut()).isEqualTo(ProcedureCheckStatus.VERIFIED);
    }

    // ---- requalification → event publié ----

    @Test
    void createChecksWithVerifiedPropagation_effectiveRequalification_publishesEvent() {
        UUID newAnalysisId = UUID.randomUUID();
        UUID prevAnalysisId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);
        caseFile.setWorkspace(new Workspace());

        CaseAnalysis newAnalysis = new CaseAnalysis();
        newAnalysis.setId(newAnalysisId);
        newAnalysis.setCaseFile(caseFile);

        ProcedureCheck verifiedCheck = new ProcedureCheck();
        verifiedCheck.setDescription("Entretien préalable tenu");
        verifiedCheck.setStatut(ProcedureCheckStatus.VERIFIED);

        when(procedureCheckRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(prevAnalysisId, ProcedureCheckStatus.VERIFIED))
                .thenReturn(List.of(verifiedCheck));
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(newAnalysisId))
                .thenReturn(List.of());
        when(procedureCheckRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseFileRepository.findTitleById(caseFileId)).thenReturn(Optional.of("Affaire Dupont"));
        when(caseFileRepository.findCreatorEmailById(caseFileId)).thenReturn(Optional.of("avocat@cabinet.fr"));

        String json = """
                {"points_procedure": [], "checks_a_requalifier": [
                    {"description": "Entretien préalable tenu", "nouveau_statut": "NON_COMPLIANT", "raison": "Contredit par pièce 3"}
                ]}""";
        service.createChecksWithVerifiedPropagation(newAnalysis, json, prevAnalysisId);

        ArgumentCaptor<ProcedureCheckRequalifiedEvent> captor = ArgumentCaptor.forClass(ProcedureCheckRequalifiedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ProcedureCheckRequalifiedEvent event = captor.getValue();
        assertThat(event.caseFileId()).isEqualTo(caseFileId);
        assertThat(event.creatorEmail()).isEqualTo("avocat@cabinet.fr");
        assertThat(event.requalifiedChecks()).hasSize(1);
        assertThat(event.requalifiedChecks().get(0).description()).isEqualTo("Entretien préalable tenu");
        assertThat(event.requalifiedChecks().get(0).raison()).isEqualTo("Contredit par pièce 3");
    }

    @Test
    void createChecksWithVerifiedPropagation_noRequalification_noEventPublished() {
        UUID newAnalysisId = UUID.randomUUID();
        UUID prevAnalysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(new Workspace());

        CaseAnalysis newAnalysis = new CaseAnalysis();
        newAnalysis.setId(newAnalysisId);
        newAnalysis.setCaseFile(caseFile);

        ProcedureCheck verifiedCheck = new ProcedureCheck();
        verifiedCheck.setDescription("Entretien préalable tenu");
        verifiedCheck.setStatut(ProcedureCheckStatus.VERIFIED);

        when(procedureCheckRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(prevAnalysisId, ProcedureCheckStatus.VERIFIED))
                .thenReturn(List.of(verifiedCheck));
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(newAnalysisId))
                .thenReturn(List.of());

        String json = "{\"points_procedure\": [], \"checks_a_requalifier\": []}";
        service.createChecksWithVerifiedPropagation(newAnalysis, json, prevAnalysisId);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void createChecksWithVerifiedPropagation_requalificationNoMatchingVerified_noEventPublished() {
        UUID newAnalysisId = UUID.randomUUID();
        UUID prevAnalysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(new Workspace());

        CaseAnalysis newAnalysis = new CaseAnalysis();
        newAnalysis.setId(newAnalysisId);
        newAnalysis.setCaseFile(caseFile);

        ProcedureCheck verifiedCheck = new ProcedureCheck();
        verifiedCheck.setDescription("Description différente");
        verifiedCheck.setStatut(ProcedureCheckStatus.VERIFIED);

        when(procedureCheckRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(prevAnalysisId, ProcedureCheckStatus.VERIFIED))
                .thenReturn(List.of(verifiedCheck));
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(newAnalysisId))
                .thenReturn(List.of());

        String json = """
                {"points_procedure": [], "checks_a_requalifier": [
                    {"description": "Description inconnue", "nouveau_statut": "NON_COMPLIANT", "raison": "raison"}
                ]}""";
        service.createChecksWithVerifiedPropagation(newAnalysis, json, prevAnalysisId);

        verify(eventPublisher, never()).publishEvent(any());
    }

    // ---- fix #1/#2 : déduplication VERIFIED ----

    @Test
    void createChecksWithVerifiedPropagation_verifiedWithSameDescriptionAsNewCheck_notDuplicated() {
        UUID newAnalysisId = UUID.randomUUID();
        UUID prevAnalysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(new Workspace());

        CaseAnalysis newAnalysis = new CaseAnalysis();
        newAnalysis.setId(newAnalysisId);
        newAnalysis.setCaseFile(caseFile);

        ProcedureCheck existingToCheck = new ProcedureCheck();
        existingToCheck.setDescription("Entretien préalable tenu");
        existingToCheck.setStatut(ProcedureCheckStatus.TO_CHECK);

        ProcedureCheck verifiedCheck = new ProcedureCheck();
        verifiedCheck.setDescription("Entretien préalable tenu"); // même libellé
        verifiedCheck.setStatut(ProcedureCheckStatus.VERIFIED);

        when(procedureCheckRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(prevAnalysisId, ProcedureCheckStatus.VERIFIED))
                .thenReturn(List.of(verifiedCheck));
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(newAnalysisId))
                .thenReturn(List.of(existingToCheck));

        String json = "{\"points_procedure\": [], \"checks_a_requalifier\": []}";
        service.createChecksWithVerifiedPropagation(newAnalysis, json, prevAnalysisId);

        // le VERIFIED ne doit pas être propagé en doublon
        verify(procedureCheckRepository, never()).save(argThat(
                c -> c instanceof ProcedureCheck pc
                        && ProcedureCheckStatus.VERIFIED == pc.getStatut()
                        && "Entretien préalable tenu".equals(pc.getDescription())));
    }

    // ---- fix #3 : propagation NON_COMPLIANT ----

    @Test
    void createChecksWithVerifiedPropagation_nonCompliantWithNoMatchingNew_propagatedAsNewCheck() {
        UUID newAnalysisId = UUID.randomUUID();
        UUID prevAnalysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(new Workspace());

        CaseAnalysis newAnalysis = new CaseAnalysis();
        newAnalysis.setId(newAnalysisId);
        newAnalysis.setCaseFile(caseFile);

        ProcedureCheck nonCompliantCheck = new ProcedureCheck();
        nonCompliantCheck.setDescription("Lettre motivée envoyée");
        nonCompliantCheck.setStatut(ProcedureCheckStatus.NON_COMPLIANT);

        when(procedureCheckRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(prevAnalysisId, ProcedureCheckStatus.NON_COMPLIANT))
                .thenReturn(List.of(nonCompliantCheck));
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(newAnalysisId))
                .thenReturn(List.of());

        String json = "{\"points_procedure\": [], \"checks_a_requalifier\": []}";
        service.createChecksWithVerifiedPropagation(newAnalysis, json, prevAnalysisId);

        ArgumentCaptor<ProcedureCheck> captor = ArgumentCaptor.forClass(ProcedureCheck.class);
        verify(procedureCheckRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(c ->
                ProcedureCheckStatus.NON_COMPLIANT == c.getStatut()
                        && "Lettre motivée envoyée".equals(c.getDescription()));
    }

    @Test
    void createChecksWithVerifiedPropagation_nonCompliantMatchingExistingToCheck_upgradesToNonCompliant() {
        UUID newAnalysisId = UUID.randomUUID();
        UUID prevAnalysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(new Workspace());

        CaseAnalysis newAnalysis = new CaseAnalysis();
        newAnalysis.setId(newAnalysisId);
        newAnalysis.setCaseFile(caseFile);

        ProcedureCheck existingToCheck = new ProcedureCheck();
        existingToCheck.setDescription("Entretien préalable tenu");
        existingToCheck.setStatut(ProcedureCheckStatus.TO_CHECK);

        ProcedureCheck nonCompliantCheck = new ProcedureCheck();
        nonCompliantCheck.setDescription("Entretien préalable tenu"); // même libellé
        nonCompliantCheck.setStatut(ProcedureCheckStatus.NON_COMPLIANT);

        when(procedureCheckRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(prevAnalysisId, ProcedureCheckStatus.NON_COMPLIANT))
                .thenReturn(List.of(nonCompliantCheck));
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(newAnalysisId))
                .thenReturn(List.of(existingToCheck));
        when(procedureCheckRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String json = "{\"points_procedure\": [], \"checks_a_requalifier\": []}";
        service.createChecksWithVerifiedPropagation(newAnalysis, json, prevAnalysisId);

        assertThat(existingToCheck.getStatut()).isEqualTo(ProcedureCheckStatus.NON_COMPLIANT);
    }

    // ---- fix #4 : matching normalisé ----

    @Test
    void createChecksWithVerifiedPropagation_requalificationCaseInsensitiveMatch_appliesRequalification() {
        UUID newAnalysisId = UUID.randomUUID();
        UUID prevAnalysisId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(new Workspace());

        CaseAnalysis newAnalysis = new CaseAnalysis();
        newAnalysis.setId(newAnalysisId);
        newAnalysis.setCaseFile(caseFile);

        ProcedureCheck verifiedCheck = new ProcedureCheck();
        verifiedCheck.setDescription("Entretien préalable tenu");
        verifiedCheck.setStatut(ProcedureCheckStatus.VERIFIED);

        when(procedureCheckRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(prevAnalysisId, ProcedureCheckStatus.VERIFIED))
                .thenReturn(List.of(verifiedCheck));
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(newAnalysisId))
                .thenReturn(List.of());
        when(procedureCheckRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // description avec casse et espaces différents
        String json = """
                {"points_procedure": [], "checks_a_requalifier": [
                    {"description": "  ENTRETIEN PRÉALABLE TENU  ", "nouveau_statut": "NON_COMPLIANT", "raison": "raison"}
                ]}""";
        service.createChecksWithVerifiedPropagation(newAnalysis, json, prevAnalysisId);

        assertThat(verifiedCheck.getStatut()).isEqualTo(ProcedureCheckStatus.NON_COMPLIANT);
    }

    // ---- updateStatus ----

    @Test
    void updateStatus_invalidStatut_throws400() {
        assertThatThrownBy(() ->
                service.updateStatus(UUID.randomUUID(), "INVALID_STATUS", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Statut invalide");
    }
}
