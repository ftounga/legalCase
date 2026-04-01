package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

    private final ProcedureCheckService service = new ProcedureCheckService(
            procedureCheckRepository, caseAnalysisRepository, caseFileRepository,
            workspaceMemberRepository, currentUserResolver);

    // ---- createChecks ----

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

    // ---- updateStatus ----

    @Test
    void updateStatus_invalidStatut_throws400() {
        assertThatThrownBy(() ->
                service.updateStatus(UUID.randomUUID(), "INVALID_STATUS", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Statut invalide");
    }
}
