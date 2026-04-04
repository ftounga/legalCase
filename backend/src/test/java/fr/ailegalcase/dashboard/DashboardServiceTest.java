package fr.ailegalcase.dashboard;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnalysisType;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.ProcedureCheck;
import fr.ailegalcase.analysis.ProcedureCheckRepository;
import fr.ailegalcase.analysis.ProcedureCheckStatus;
import fr.ailegalcase.casefile.CaseDeadline;
import fr.ailegalcase.casefile.CaseDeadlineRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private CaseFileRepository caseFileRepository;
    @Mock private CaseDeadlineRepository caseDeadlineRepository;
    @Mock private ProcedureCheckRepository procedureCheckRepository;
    @Mock private CaseAnalysisRepository caseAnalysisRepository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(caseFileRepository, caseDeadlineRepository,
                procedureCheckRepository, caseAnalysisRepository);
    }

    private Workspace workspace(UUID id) {
        Workspace ws = new Workspace();
        ws.setId(id);
        return ws;
    }

    private CaseFile caseFile(UUID id, String title, Workspace ws) {
        CaseFile cf = new CaseFile();
        cf.setId(id);
        cf.setTitle(title);
        cf.setLegalDomain("DROIT_DU_TRAVAIL");
        cf.setStatus("ACTIVE");
        cf.setWorkspace(ws);
        return cf;
    }

    private CaseDeadline deadline(UUID id, LocalDate dueDate, CaseFile cf) {
        CaseDeadline d = new CaseDeadline();
        d.setId(id);
        d.setLabel("Délai test");
        d.setDueDate(dueDate);
        d.setCaseFile(cf);
        return d;
    }

    private ProcedureCheck staleCheck(UUID id, CaseFile cf, Instant updatedAt) {
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(cf);
        ProcedureCheck pc = new ProcedureCheck();
        pc.setId(id);
        pc.setStatut(ProcedureCheckStatus.NON_COMPLIANT);
        pc.setCaseAnalysis(analysis);
        return pc;
    }

    private CaseAnalysis analysis(UUID id, CaseFile cf) {
        CaseAnalysis ca = new CaseAnalysis();
        ca.setId(id);
        ca.setCaseFile(cf);
        ca.setAnalysisType(AnalysisType.STANDARD);
        ca.setAnalysisStatus(AnalysisStatus.DONE);
        ca.setVersion(1);
        return ca;
    }

    // DASH-01 : workspace vide → toutes les listes vides, openCasesCount = 0
    @Test
    void buildSummary_emptyWorkspace_returnsAllEmpty() {
        Workspace ws = workspace(UUID.randomUUID());
        when(caseFileRepository.findTop20ByWorkspaceAndDeletedAtIsNullAndStatusNotOrderByCreatedAtDesc(ws, "CLOSED"))
                .thenReturn(List.of());
        when(caseFileRepository.countByWorkspaceAndDeletedAtIsNullAndStatusNot(ws, "CLOSED"))
                .thenReturn(0L);
        when(caseDeadlineRepository.findUrgentByWorkspaceId(eq(ws.getId()), any()))
                .thenReturn(List.of());
        when(procedureCheckRepository.findStaleNonCompliantByWorkspaceId(eq(ws.getId()), any()))
                .thenReturn(List.of());
        when(caseAnalysisRepository.findTop5ByCaseFile_WorkspaceAndAnalysisStatusOrderByCreatedAtDesc(ws, AnalysisStatus.DONE))
                .thenReturn(List.of());

        DashboardSummaryResponse result = service.buildSummary(ws);

        assertThat(result.openCases()).isEmpty();
        assertThat(result.openCasesCount()).isZero();
        assertThat(result.urgentDeadlines()).isEmpty();
        assertThat(result.staleChecks()).isEmpty();
        assertThat(result.recentAnalyses()).isEmpty();
    }

    // DASH-02 : urgentDeadlines — délai à J+6 inclus, délai à J+8 exclu (filtrage fait par la requête)
    @Test
    void buildSummary_urgentDeadlines_onlyReturnsDeadlinesFromRepository() {
        Workspace ws = workspace(UUID.randomUUID());
        CaseFile cf = caseFile(UUID.randomUUID(), "Dossier A", ws);
        CaseDeadline urgent = deadline(UUID.randomUUID(), LocalDate.now().plusDays(6), cf);

        when(caseFileRepository.findTop20ByWorkspaceAndDeletedAtIsNullAndStatusNotOrderByCreatedAtDesc(ws, "CLOSED"))
                .thenReturn(List.of());
        when(caseFileRepository.countByWorkspaceAndDeletedAtIsNullAndStatusNot(ws, "CLOSED"))
                .thenReturn(0L);
        when(caseDeadlineRepository.findUrgentByWorkspaceId(eq(ws.getId()), any()))
                .thenReturn(List.of(urgent));
        when(procedureCheckRepository.findStaleNonCompliantByWorkspaceId(eq(ws.getId()), any()))
                .thenReturn(List.of());
        when(caseAnalysisRepository.findTop5ByCaseFile_WorkspaceAndAnalysisStatusOrderByCreatedAtDesc(ws, AnalysisStatus.DONE))
                .thenReturn(List.of());

        DashboardSummaryResponse result = service.buildSummary(ws);

        assertThat(result.urgentDeadlines()).hasSize(1);
        assertThat(result.urgentDeadlines().get(0).label()).isEqualTo("Délai test");
        assertThat(result.urgentDeadlines().get(0).caseFileTitle()).isEqualTo("Dossier A");
    }

    // DASH-03 : staleChecks — check NON_COMPLIANT retourné depuis le repository, groupé par dossier
    @Test
    void buildSummary_staleChecks_groupedByCaseFile() {
        Workspace ws = workspace(UUID.randomUUID());
        CaseFile cf = caseFile(UUID.randomUUID(), "Dossier B", ws);
        Instant staleTime = Instant.now().minus(8, ChronoUnit.DAYS);
        ProcedureCheck pc1 = staleCheck(UUID.randomUUID(), cf, staleTime);
        ProcedureCheck pc2 = staleCheck(UUID.randomUUID(), cf, staleTime);

        when(caseFileRepository.findTop20ByWorkspaceAndDeletedAtIsNullAndStatusNotOrderByCreatedAtDesc(ws, "CLOSED"))
                .thenReturn(List.of());
        when(caseFileRepository.countByWorkspaceAndDeletedAtIsNullAndStatusNot(ws, "CLOSED"))
                .thenReturn(0L);
        when(caseDeadlineRepository.findUrgentByWorkspaceId(eq(ws.getId()), any()))
                .thenReturn(List.of());
        when(procedureCheckRepository.findStaleNonCompliantByWorkspaceId(eq(ws.getId()), any()))
                .thenReturn(List.of(pc1, pc2));
        when(caseAnalysisRepository.findTop5ByCaseFile_WorkspaceAndAnalysisStatusOrderByCreatedAtDesc(ws, AnalysisStatus.DONE))
                .thenReturn(List.of());

        DashboardSummaryResponse result = service.buildSummary(ws);

        assertThat(result.staleChecks()).hasSize(1);
        assertThat(result.staleChecks().get(0).caseFileTitle()).isEqualTo("Dossier B");
        assertThat(result.staleChecks().get(0).nonCompliantCount()).isEqualTo(2L);
    }

    // DASH-04 : openCases — seuls les dossiers actifs retournés depuis le repository
    @Test
    void buildSummary_openCases_mapsCorrectly() {
        Workspace ws = workspace(UUID.randomUUID());
        CaseFile cf = caseFile(UUID.randomUUID(), "Dossier actif", ws);

        when(caseFileRepository.findTop20ByWorkspaceAndDeletedAtIsNullAndStatusNotOrderByCreatedAtDesc(ws, "CLOSED"))
                .thenReturn(List.of(cf));
        when(caseFileRepository.countByWorkspaceAndDeletedAtIsNullAndStatusNot(ws, "CLOSED"))
                .thenReturn(1L);
        when(caseDeadlineRepository.findUrgentByWorkspaceId(eq(ws.getId()), any()))
                .thenReturn(List.of());
        when(procedureCheckRepository.findStaleNonCompliantByWorkspaceId(eq(ws.getId()), any()))
                .thenReturn(List.of());
        when(caseAnalysisRepository.findTop5ByCaseFile_WorkspaceAndAnalysisStatusOrderByCreatedAtDesc(ws, AnalysisStatus.DONE))
                .thenReturn(List.of());

        DashboardSummaryResponse result = service.buildSummary(ws);

        assertThat(result.openCases()).hasSize(1);
        assertThat(result.openCases().get(0).title()).isEqualTo("Dossier actif");
        assertThat(result.openCases().get(0).status()).isEqualTo("ACTIVE");
        assertThat(result.openCasesCount()).isEqualTo(1L);
    }
}
