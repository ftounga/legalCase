package fr.ailegalcase.dashboard;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnalysisType;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.ProcedureCheck;
import fr.ailegalcase.analysis.ProcedureCheckRepository;
import fr.ailegalcase.analysis.ProcedureCheckStatus;
import fr.ailegalcase.auth.User;
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
import java.time.ZoneId;
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

    private User user(String firstName) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("test@example.com");
        u.setStatus("ACTIVE");
        u.setFirstName(firstName);
        return u;
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

    private CaseAnalysis analysisWithCreatedAt(UUID id, CaseFile cf, Instant createdAt) {
        CaseAnalysis ca = analysis(id, cf);
        ca.setCreatedAt(createdAt);
        return ca;
    }

    /** Stub commun pour les méthodes non liées à la feature testée. */
    private void stubDefaults(Workspace ws) {
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
        when(caseFileRepository.countByWorkspaceAndDeletedAtIsNullAndCreatedAtAfter(eq(ws), any()))
                .thenReturn(0L);
        when(caseAnalysisRepository.findByCaseFile_WorkspaceAndAnalysisStatusAndCreatedAtAfter(eq(ws), eq(AnalysisStatus.DONE), any()))
                .thenReturn(List.of());
    }

    // DASH-01 : workspace vide → toutes les listes vides, openCasesCount = 0
    @Test
    void buildSummary_emptyWorkspace_returnsAllEmpty() {
        Workspace ws = workspace(UUID.randomUUID());
        stubDefaults(ws);

        DashboardSummaryResponse result = service.buildSummary(ws, user("Marie"));

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
        when(caseFileRepository.countByWorkspaceAndDeletedAtIsNullAndCreatedAtAfter(eq(ws), any()))
                .thenReturn(0L);
        when(caseAnalysisRepository.findByCaseFile_WorkspaceAndAnalysisStatusAndCreatedAtAfter(eq(ws), eq(AnalysisStatus.DONE), any()))
                .thenReturn(List.of());

        DashboardSummaryResponse result = service.buildSummary(ws, user("Marie"));

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
        when(caseFileRepository.countByWorkspaceAndDeletedAtIsNullAndCreatedAtAfter(eq(ws), any()))
                .thenReturn(0L);
        when(caseAnalysisRepository.findByCaseFile_WorkspaceAndAnalysisStatusAndCreatedAtAfter(eq(ws), eq(AnalysisStatus.DONE), any()))
                .thenReturn(List.of());

        DashboardSummaryResponse result = service.buildSummary(ws, user("Marie"));

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
        when(caseFileRepository.countByWorkspaceAndDeletedAtIsNullAndCreatedAtAfter(eq(ws), any()))
                .thenReturn(0L);
        when(caseAnalysisRepository.findByCaseFile_WorkspaceAndAnalysisStatusAndCreatedAtAfter(eq(ws), eq(AnalysisStatus.DONE), any()))
                .thenReturn(List.of());

        DashboardSummaryResponse result = service.buildSummary(ws, user("Marie"));

        assertThat(result.openCases()).hasSize(1);
        assertThat(result.openCases().get(0).title()).isEqualTo("Dossier actif");
        assertThat(result.openCases().get(0).status()).isEqualTo("ACTIVE");
        assertThat(result.openCasesCount()).isEqualTo(1L);
    }

    // DASH-05 : weeklyActivity — toujours 7 entrées, ordonnées J-6 → J0
    @Test
    void buildSummary_weeklyActivity_alwaysSevenEntriesOrderedOldestFirst() {
        Workspace ws = workspace(UUID.randomUUID());
        stubDefaults(ws);

        DashboardSummaryResponse result = service.buildSummary(ws, user("Marie"));

        assertThat(result.weeklyActivity()).hasSize(7);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        for (int i = 0; i < 7; i++) {
            assertThat(result.weeklyActivity().get(i).date())
                    .isEqualTo(today.minusDays(6 - i));
        }
    }

    // DASH-06 : weeklyActivity — jour sans analyse → analysesCount = 0
    @Test
    void buildSummary_weeklyActivity_dayWithNoAnalysis_hasZeroCount() {
        Workspace ws = workspace(UUID.randomUUID());
        stubDefaults(ws);

        DashboardSummaryResponse result = service.buildSummary(ws, user("Marie"));

        assertThat(result.weeklyActivity()).allSatisfy(item ->
                assertThat(item.analysesCount()).isGreaterThanOrEqualTo(0L));
        // All days have 0 count when no analyses returned
        assertThat(result.weeklyActivity()).allSatisfy(item ->
                assertThat(item.analysesCount()).isEqualTo(0L));
    }

    // DASH-07 : weeklyActivity — bucketing par jour correct
    @Test
    void buildSummary_weeklyActivity_bucketingByDayIsCorrect() {
        Workspace ws = workspace(UUID.randomUUID());
        CaseFile cf = caseFile(UUID.randomUUID(), "Dossier X", ws);

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        // 2 analyses on J-2, 1 analysis on J-0 (today)
        Instant dayMinus2 = today.minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant().plusSeconds(3600);
        Instant dayToday = today.atStartOfDay(ZoneId.systemDefault()).toInstant().plusSeconds(3600);

        CaseAnalysis a1 = analysisWithCreatedAt(UUID.randomUUID(), cf, dayMinus2);
        CaseAnalysis a2 = analysisWithCreatedAt(UUID.randomUUID(), cf, dayMinus2);
        CaseAnalysis a3 = analysisWithCreatedAt(UUID.randomUUID(), cf, dayToday);

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
        when(caseFileRepository.countByWorkspaceAndDeletedAtIsNullAndCreatedAtAfter(eq(ws), any()))
                .thenReturn(0L);
        when(caseAnalysisRepository.findByCaseFile_WorkspaceAndAnalysisStatusAndCreatedAtAfter(eq(ws), eq(AnalysisStatus.DONE), any()))
                .thenReturn(List.of(a1, a2, a3));

        DashboardSummaryResponse result = service.buildSummary(ws, user("Marie"));

        assertThat(result.weeklyActivity()).hasSize(7);
        // J-2 has 2 analyses
        DashboardActivityDayItem dayMinus2Item = result.weeklyActivity().get(4); // index 4 = J-2 (J-6,J-5,J-4,J-3,J-2,J-1,J0)
        assertThat(dayMinus2Item.date()).isEqualTo(today.minusDays(2));
        assertThat(dayMinus2Item.analysesCount()).isEqualTo(2L);
        // today has 1 analysis
        DashboardActivityDayItem todayItem = result.weeklyActivity().get(6);
        assertThat(todayItem.date()).isEqualTo(today);
        assertThat(todayItem.analysesCount()).isEqualTo(1L);
        // other days have 0
        assertThat(result.weeklyActivity().get(0).analysesCount()).isEqualTo(0L);
        assertThat(result.weeklyActivity().get(1).analysesCount()).isEqualTo(0L);
        assertThat(result.weeklyActivity().get(2).analysesCount()).isEqualTo(0L);
        assertThat(result.weeklyActivity().get(3).analysesCount()).isEqualTo(0L);
        assertThat(result.weeklyActivity().get(5).analysesCount()).isEqualTo(0L);
    }

    // DASH-08 : casesOpenedThisWeek — compte les dossiers créés < 7j, exclut les autres (logique déléguée au repo)
    @Test
    void buildSummary_casesOpenedThisWeek_returnsValueFromRepository() {
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
        when(caseFileRepository.countByWorkspaceAndDeletedAtIsNullAndCreatedAtAfter(eq(ws), any()))
                .thenReturn(3L);
        when(caseAnalysisRepository.findByCaseFile_WorkspaceAndAnalysisStatusAndCreatedAtAfter(eq(ws), eq(AnalysisStatus.DONE), any()))
                .thenReturn(List.of());

        DashboardSummaryResponse result = service.buildSummary(ws, user("Marie"));

        assertThat(result.casesOpenedThisWeek()).isEqualTo(3L);
    }

    // DASH-09 : userFirstName — repris du User passé en paramètre
    @Test
    void buildSummary_userFirstName_returnedFromUser() {
        Workspace ws = workspace(UUID.randomUUID());
        stubDefaults(ws);

        DashboardSummaryResponse result = service.buildSummary(ws, user("Sophie"));

        assertThat(result.userFirstName()).isEqualTo("Sophie");
    }

    // DASH-10 : userFirstName null — géré sans exception
    @Test
    void buildSummary_userFirstName_nullIsHandledGracefully() {
        Workspace ws = workspace(UUID.randomUUID());
        stubDefaults(ws);

        DashboardSummaryResponse result = service.buildSummary(ws, user(null));

        assertThat(result.userFirstName()).isNull();
    }

    // DASH-11 : userFirstName — null user passé → null renvoyé sans exception
    @Test
    void buildSummary_nullUser_returnsNullFirstName() {
        Workspace ws = workspace(UUID.randomUUID());
        stubDefaults(ws);

        DashboardSummaryResponse result = service.buildSummary(ws, null);

        assertThat(result.userFirstName()).isNull();
    }
}
