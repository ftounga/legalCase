package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.CaseDeadline;
import fr.ailegalcase.casefile.CaseDeadlineRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.ImmigrationTitleDecision;
import fr.ailegalcase.casefile.ImmigrationTitleDecisionRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-193 SF-193-01 — UT du service d'alignement (matérialisation + lecture).
 *
 * <p>Pattern miroir {@link RetainedPisteAlignmentServiceTest} (F-192).</p>
 */
class ProcedureCheckAlignmentServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 5, 6);

    private ProcedureCheckRepository procedureCheckRepository;
    private CaseAnalysisRepository caseAnalysisRepository;
    private CaseFileRepository caseFileRepository;
    private CaseDeadlineRepository caseDeadlineRepository;
    private ImmigrationTitleDecisionRepository titleDecisionRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private CurrentUserResolver currentUserResolver;
    private Clock fixedClock;

    private ProcedureCheckAlignmentService service;

    @BeforeEach
    void setUp() {
        procedureCheckRepository = mock(ProcedureCheckRepository.class);
        caseAnalysisRepository = mock(CaseAnalysisRepository.class);
        caseFileRepository = mock(CaseFileRepository.class);
        caseDeadlineRepository = mock(CaseDeadlineRepository.class);
        titleDecisionRepository = mock(ImmigrationTitleDecisionRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        currentUserResolver = mock(CurrentUserResolver.class);
        fixedClock = Clock.fixed(FIXED_DATE.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

        service = new ProcedureCheckAlignmentService(
                procedureCheckRepository, caseAnalysisRepository, caseFileRepository,
                caseDeadlineRepository, titleDecisionRepository, workspaceMemberRepository,
                currentUserResolver, fixedClock);
    }

    @Test
    void materializeForAnalysis_emptyChecks_doesNothing() {
        CaseAnalysis analysis = newAnalysis();
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysis.getId()))
                .thenReturn(List.of());

        service.materializeForAnalysis(analysis);

        verify(caseAnalysisRepository, never()).save(any());
        verify(caseDeadlineRepository, never()).save(any());
    }

    @Test
    void materializeForAnalysis_verifiedIM05MotifMatching_persistsAligned() {
        CaseAnalysis analysis = newAnalysis();
        UUID caseFileId = analysis.getCaseFile().getId();
        ProcedureCheck check = newCheck(ProcedureCheckStatus.VERIFIED, "IM05_MOTIF", "TRAVAIL",
                "Motif de séjour identifié");

        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysis.getId()))
                .thenReturn(List.of(check));

        ImmigrationTitleDecision decision = new ImmigrationTitleDecision();
        decision.setMotif("TRAVAIL");
        when(titleDecisionRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.of(decision));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getProcedureChecksAlignmentJson();
        assertThat(json).contains("\"matchStatus\":\"ALIGNED\"")
                .contains("\"toolIdCible\":\"" + ProcedureCheckToolMatcher.TOOL_IM_05_TITRE + "\"");
    }

    @Test
    void materializeForAnalysis_verifiedIM05MotifMismatch_persistsDivergent() {
        CaseAnalysis analysis = newAnalysis();
        UUID caseFileId = analysis.getCaseFile().getId();
        ProcedureCheck check = newCheck(ProcedureCheckStatus.VERIFIED, "IM05_MOTIF", "FAMILLE",
                "Motif");

        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysis.getId()))
                .thenReturn(List.of(check));

        ImmigrationTitleDecision decision = new ImmigrationTitleDecision();
        decision.setMotif("TRAVAIL");
        when(titleDecisionRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.of(decision));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getProcedureChecksAlignmentJson())
                .contains("\"matchStatus\":\"DIVERGENT\"");
    }

    @Test
    void materializeForAnalysis_nonCompliant_persistsNonCompliantFlag() {
        CaseAnalysis analysis = newAnalysis();
        ProcedureCheck check = newCheck(ProcedureCheckStatus.NON_COMPLIANT, "FR_CONVOCATION",
                null, "Pas de convocation à entretien préalable");

        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysis.getId()))
                .thenReturn(List.of(check));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getProcedureChecksAlignmentJson();
        assertThat(json).contains("\"matchStatus\":\"NON_COMPLIANT_FLAG\"")
                .contains("\"toolIdCible\":\"" + ProcedureCheckToolMatcher.TOOL_DT_08_LICENCIEMENT + "\"");
    }

    @Test
    void materializeForAnalysis_toCheck_persistsToVerifyFlag() {
        CaseAnalysis analysis = newAnalysis();
        ProcedureCheck check = newCheck(ProcedureCheckStatus.TO_CHECK, "FR_CONVOCATION",
                null, "Convocation à vérifier");

        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysis.getId()))
                .thenReturn(List.of(check));
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any())).thenReturn(new ArrayList<>());

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getProcedureChecksAlignmentJson())
                .contains("\"matchStatus\":\"TO_VERIFY_FLAG\"");
    }

    @Test
    void materializeForAnalysis_unknownCritereCode_persistsNoTargetTool() {
        CaseAnalysis analysis = newAnalysis();
        ProcedureCheck check = newCheck(ProcedureCheckStatus.VERIFIED, "UNKNOWN_CODE", null,
                "Check libre");

        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysis.getId()))
                .thenReturn(List.of(check));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getProcedureChecksAlignmentJson();
        assertThat(json).contains("\"matchStatus\":\"NO_TARGET_TOOL\"");
        assertThat(json).doesNotContain("\"toolIdCible\":\"");
    }

    @Test
    void materializeForAnalysis_verifiedIM05ButNoToolAnalyzed_persistsNotAnalyzed() {
        CaseAnalysis analysis = newAnalysis();
        UUID caseFileId = analysis.getCaseFile().getId();
        ProcedureCheck check = newCheck(ProcedureCheckStatus.VERIFIED, "IM05_MOTIF", "TRAVAIL",
                "Motif");

        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysis.getId()))
                .thenReturn(List.of(check));
        when(titleDecisionRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.empty());

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getProcedureChecksAlignmentJson())
                .contains("\"matchStatus\":\"NOT_ANALYZED\"");
    }

    @Test
    void materializeForAnalysis_verifiedIM05RepoThrows_failsOpenWithNotAnalyzed() {
        CaseAnalysis analysis = newAnalysis();
        UUID caseFileId = analysis.getCaseFile().getId();
        ProcedureCheck check = newCheck(ProcedureCheckStatus.VERIFIED, "IM05_MOTIF", "TRAVAIL",
                "Motif");

        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysis.getId()))
                .thenReturn(List.of(check));
        when(titleDecisionRepository.findByCaseFileId(caseFileId))
                .thenThrow(new RuntimeException("DB down"));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getProcedureChecksAlignmentJson())
                .contains("\"matchStatus\":\"NOT_ANALYZED\"");
    }

    @Test
    void materializeForAnalysis_pieces_idempotent_2runs_1entry() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");
        ProcedureCheck check = newCheck(ProcedureCheckStatus.NON_COMPLIANT, "FR_DELAI_NOTIFICATION",
                null, "Notification écrite manquante");

        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysis.getId()))
                .thenReturn(List.of(check));

        // 1er run
        service.materializeForAnalysis(analysis);
        assertThat(analysis.getAnalysisResult()).contains("Notification écrite manquante");
        assertThat(analysis.getAnalysisResult()).contains("PROCEDURE_CHECK_NON_COMPLIANT");

        // 2e run avec le même état
        service.materializeForAnalysis(analysis);
        long count = analysis.getAnalysisResult().split("Notification écrite manquante", -1).length - 1;
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void materializeForAnalysis_verifiedDoesNotPropagateToPieces() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");
        ProcedureCheck check = newCheck(ProcedureCheckStatus.VERIFIED, "FR_CONVOCATION",
                null, "Convocation conforme");

        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysis.getId()))
                .thenReturn(List.of(check));

        service.materializeForAnalysis(analysis);

        // Pas de propagation pour VERIFIED — analysis_result inchangé sauf alignment
        assertThat(analysis.getAnalysisResult()).doesNotContain("PROCEDURE_CHECK_NON_COMPLIANT");
    }

    @Test
    void materializeForAnalysis_toCheckWithKnownDelai_createsDeadline() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{}");
        ProcedureCheck check = newCheck(ProcedureCheckStatus.TO_CHECK, "FR_DELAI_NOTIFICATION",
                null, "Délai à vérifier");

        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysis.getId()))
                .thenReturn(List.of(check));
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any())).thenReturn(new ArrayList<>());

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseDeadline> savedDeadline = ArgumentCaptor.forClass(CaseDeadline.class);
        verify(caseDeadlineRepository, times(1)).save(savedDeadline.capture());
        assertThat(savedDeadline.getValue().getSource()).isEqualTo("PROCEDURE_CHECK_TO_CHECK");
        assertThat(savedDeadline.getValue().getLabel()).startsWith("À vérifier : Délai à vérifier");
        // FR_DELAI_NOTIFICATION = 12 mois
        assertThat(savedDeadline.getValue().getDueDate()).isEqualTo(FIXED_DATE.plusMonths(12));
    }

    @Test
    void materializeForAnalysis_toCheckWithoutKnownDelai_noDeadlineCreated() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{}");
        ProcedureCheck check = newCheck(ProcedureCheckStatus.TO_CHECK, "BE_NOTIFICATION",
                null, "Délai BE non connu V1");

        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysis.getId()))
                .thenReturn(List.of(check));
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any())).thenReturn(new ArrayList<>());

        service.materializeForAnalysis(analysis);

        verify(caseDeadlineRepository, never()).save(any());
    }

    @Test
    void materializeForAnalysis_deadlinesIdempotent() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{}");
        ProcedureCheck check = newCheck(ProcedureCheckStatus.TO_CHECK, "FR_DELAI_NOTIFICATION",
                null, "Délai à vérifier");

        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysis.getId()))
                .thenReturn(List.of(check));

        // 1er run : aucun délai existant
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any())).thenReturn(new ArrayList<>());
        service.materializeForAnalysis(analysis);
        verify(caseDeadlineRepository, times(1)).save(any());

        // 2e run : on simule que le délai créé au 1er run est maintenant en base
        CaseDeadline existing = new CaseDeadline();
        existing.setSource("PROCEDURE_CHECK_TO_CHECK");
        existing.setLabel("À vérifier : Délai à vérifier");
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any())).thenReturn(List.of(existing));
        service.materializeForAnalysis(analysis);
        // Pas de nouveau save — total reste à 1
        verify(caseDeadlineRepository, times(1)).save(any());
    }

    @Test
    void getForLatestAnalysis_legacyAnalysisWithoutJson_returnsEmptyList() {
        // Test direct du helper deserializeAlignment
        assertThat(service.deserializeAlignment(null)).isEmpty();
        assertThat(service.deserializeAlignment("")).isEmpty();
        assertThat(service.deserializeAlignment("[]")).isEmpty();
    }

    @Test
    void deserializeAlignment_validJson_returnsList() throws Exception {
        List<ProcedureCheckAlignment> list = List.of(new ProcedureCheckAlignment(
                UUID.randomUUID(), "Convocation", "FR_CONVOCATION", "VERIFIED",
                null, null, ProcedureCheckToolMatcher.TOOL_DT_08_LICENCIEMENT,
                ProcedureCheckAlignment.STATUS_ALIGNED));
        String json = MAPPER.writeValueAsString(list);

        List<ProcedureCheckAlignment> got = service.deserializeAlignment(json);

        assertThat(got).hasSize(1);
        assertThat(got.get(0).matchStatus()).isEqualTo(ProcedureCheckAlignment.STATUS_ALIGNED);
        assertThat(got.get(0).libelle()).isEqualTo("Convocation");
    }

    @Test
    void deserializeAlignment_invalidJson_returnsEmpty() {
        assertThat(service.deserializeAlignment("not-json")).isEmpty();
    }

    // ---- helpers ----

    private CaseAnalysis newAnalysis() {
        Workspace ws = new Workspace();
        ws.setName("Test");
        CaseFile cf = new CaseFile();
        cf.setWorkspace(ws);
        CaseAnalysis a = new CaseAnalysis();
        a.setCaseFile(cf);
        try {
            java.lang.reflect.Field idField = CaseAnalysis.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(a, UUID.randomUUID());

            java.lang.reflect.Field cfIdField = CaseFile.class.getDeclaredField("id");
            cfIdField.setAccessible(true);
            cfIdField.set(cf, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        a.setAnalysisStatus(AnalysisStatus.DONE);
        return a;
    }

    private ProcedureCheck newCheck(ProcedureCheckStatus statut, String critereCode,
                                     String expectedValue, String description) {
        ProcedureCheck c = new ProcedureCheck();
        try {
            java.lang.reflect.Field idField = ProcedureCheck.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(c, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        c.setStatut(statut);
        c.setCritereCode(critereCode);
        c.setExpectedValue(expectedValue);
        c.setDescription(description);
        return c;
    }
}
