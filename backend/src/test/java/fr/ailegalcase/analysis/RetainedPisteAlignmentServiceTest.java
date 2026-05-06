package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.CaseDeadline;
import fr.ailegalcase.casefile.CaseDeadlineRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.ImmigrationRecours;
import fr.ailegalcase.casefile.ImmigrationRecoursRepository;
import fr.ailegalcase.casefile.ImmigrationTitleDecision;
import fr.ailegalcase.casefile.ImmigrationTitleDecisionRepository;
import fr.ailegalcase.casefile.TitleRecommendation;
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
 * F-192 SF-192-01 — UT du service d'alignement (matérialisation + lecture).
 */
class RetainedPisteAlignmentServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 5, 6);

    private StrategicOptionRepository strategicOptionRepository;
    private CaseAnalysisRepository caseAnalysisRepository;
    private CaseFileRepository caseFileRepository;
    private CaseDeadlineRepository caseDeadlineRepository;
    private ImmigrationTitleDecisionRepository titleDecisionRepository;
    private ImmigrationRecoursRepository recoursRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private CurrentUserResolver currentUserResolver;
    private Clock fixedClock;

    private RetainedPisteAlignmentService service;

    @BeforeEach
    void setUp() {
        strategicOptionRepository = mock(StrategicOptionRepository.class);
        caseAnalysisRepository = mock(CaseAnalysisRepository.class);
        caseFileRepository = mock(CaseFileRepository.class);
        caseDeadlineRepository = mock(CaseDeadlineRepository.class);
        titleDecisionRepository = mock(ImmigrationTitleDecisionRepository.class);
        recoursRepository = mock(ImmigrationRecoursRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        currentUserResolver = mock(CurrentUserResolver.class);
        fixedClock = Clock.fixed(FIXED_DATE.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

        service = new RetainedPisteAlignmentService(
                strategicOptionRepository, caseAnalysisRepository, caseFileRepository,
                caseDeadlineRepository, titleDecisionRepository, recoursRepository,
                workspaceMemberRepository, currentUserResolver, fixedClock);
    }

    @Test
    void materializeForAnalysis_emptyRetained_doesNothing() {
        CaseAnalysis analysis = newAnalysis();
        when(strategicOptionRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(
                analysis.getId(), StrategicOptionStatus.RETAINED)).thenReturn(List.of());

        service.materializeForAnalysis(analysis);

        verify(caseAnalysisRepository, never()).save(any());
        verify(caseDeadlineRepository, never()).save(any());
    }

    @Test
    void materializeForAnalysis_pisteAlignedWithTitleTop3_persistsAligned() throws Exception {
        CaseAnalysis analysis = newAnalysis();
        UUID caseFileId = analysis.getCaseFile().getId();
        StrategicOption piste = newPiste("Demander un titre L.421-14",
                "Art. L.421-14 CESEDA", null);

        when(strategicOptionRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(
                analysis.getId(), StrategicOptionStatus.RETAINED))
                .thenReturn(List.of(piste));

        ImmigrationTitleDecision decision = new ImmigrationTitleDecision();
        List<TitleRecommendation> recs = List.of(
                new TitleRecommendation("CHERCHEUR", "Passeport talent — chercheur (L.421-14)",
                        "FRANCE", "TRAVAIL", "Conditions", List.of(), 30));
        decision.setRecommendedTitles(MAPPER.writeValueAsString(recs));
        when(titleDecisionRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.of(decision));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getRetainedPistesAlignmentJson();
        assertThat(json).contains("\"matchStatus\":\"ALIGNED\"")
                .contains("\"toolIdCible\":\"" + RetainedPisteToolMatcher.TOOL_TITRE + "\"");
    }

    @Test
    void materializeForAnalysis_pisteNotInTop3_persistsDivergent() throws Exception {
        CaseAnalysis analysis = newAnalysis();
        UUID caseFileId = analysis.getCaseFile().getId();
        StrategicOption piste = newPiste("Demander un titre L.421-99",
                "Art. L.421-99 CESEDA", null);

        when(strategicOptionRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(
                analysis.getId(), StrategicOptionStatus.RETAINED))
                .thenReturn(List.of(piste));

        ImmigrationTitleDecision decision = new ImmigrationTitleDecision();
        List<TitleRecommendation> recs = List.of(
                new TitleRecommendation("CHERCHEUR", "Titre L.421-14", "FRANCE", "TRAVAIL", "C", List.of(), 30),
                new TitleRecommendation("VPF", "Titre L.423-1", "FRANCE", "FAMILLE", "C", List.of(), 30));
        decision.setRecommendedTitles(MAPPER.writeValueAsString(recs));
        when(titleDecisionRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.of(decision));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getRetainedPistesAlignmentJson())
                .contains("\"matchStatus\":\"DIVERGENT\"");
    }

    @Test
    void materializeForAnalysis_toolNotAnalyzed_persistsNotAnalyzed() {
        CaseAnalysis analysis = newAnalysis();
        UUID caseFileId = analysis.getCaseFile().getId();
        StrategicOption piste = newPiste("Demander un VPF", "L.423-1 CESEDA", null);

        when(strategicOptionRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(
                analysis.getId(), StrategicOptionStatus.RETAINED))
                .thenReturn(List.of(piste));
        when(titleDecisionRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.empty());

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getRetainedPistesAlignmentJson())
                .contains("\"matchStatus\":\"NOT_ANALYZED\"");
    }

    @Test
    void materializeForAnalysis_repoThrows_failsOpenWithNotAnalyzed() {
        CaseAnalysis analysis = newAnalysis();
        UUID caseFileId = analysis.getCaseFile().getId();
        StrategicOption piste = newPiste("VPF", "L.421-14 CESEDA", null);

        when(strategicOptionRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(
                analysis.getId(), StrategicOptionStatus.RETAINED))
                .thenReturn(List.of(piste));
        when(titleDecisionRepository.findByCaseFileId(caseFileId))
                .thenThrow(new RuntimeException("DB down"));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getRetainedPistesAlignmentJson())
                .contains("\"matchStatus\":\"NOT_ANALYZED\"");
    }

    @Test
    void materializeForAnalysis_pisteWithoutBaseJuridique_persistsNoTargetTool() {
        CaseAnalysis analysis = newAnalysis();
        StrategicOption piste = newPiste("Saisir le bâtonnier pour conciliation",
                null, null);

        when(strategicOptionRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(
                analysis.getId(), StrategicOptionStatus.RETAINED))
                .thenReturn(List.of(piste));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getRetainedPistesAlignmentJson())
                .contains("\"matchStatus\":\"NO_TARGET_TOOL\"");
    }

    @Test
    void materializeForAnalysis_pieces_idempotent_2runs_1entry() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");
        StrategicOption piste = newPiste("Démarche A", "L.421-14 CESEDA",
                List.of("Convention d'accueil INRIA"));

        when(strategicOptionRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(
                analysis.getId(), StrategicOptionStatus.RETAINED))
                .thenReturn(List.of(piste));
        when(titleDecisionRepository.findByCaseFileId(any())).thenReturn(Optional.empty());

        // 1er run
        service.materializeForAnalysis(analysis);
        // simule la persistance : analyse_result a maintenant la pièce
        assertThat(analysis.getAnalysisResult()).contains("Convention d'accueil INRIA");
        assertThat(analysis.getAnalysisResult()).contains("PISTE_RETENUE");

        // 2e run avec le même état
        service.materializeForAnalysis(analysis);
        long count = analysis.getAnalysisResult().split("Convention d'accueil INRIA", -1).length - 1;
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void materializeForAnalysis_horizon_3to6mois_createsDeadlineAt6mois() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{}");
        StrategicOption piste = newPiste("Démarche horizon",
                "L.421-14 CESEDA", List.of(),
                "Court terme (3-6 mois)");

        when(strategicOptionRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(
                analysis.getId(), StrategicOptionStatus.RETAINED))
                .thenReturn(List.of(piste));
        when(titleDecisionRepository.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any())).thenReturn(new ArrayList<>());

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseDeadline> savedDeadline = ArgumentCaptor.forClass(CaseDeadline.class);
        verify(caseDeadlineRepository, times(1)).save(savedDeadline.capture());
        assertThat(savedDeadline.getValue().getDueDate()).isEqualTo(FIXED_DATE.plusMonths(6));
        assertThat(savedDeadline.getValue().getSource()).isEqualTo("PISTE_RETENUE");
        assertThat(savedDeadline.getValue().getLabel()).startsWith("Stratégie : Démarche horizon");
    }

    @Test
    void materializeForAnalysis_horizonUnparsable_noDeadlineCreated() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{}");
        StrategicOption piste = newPiste("Sans horizon clair",
                "L.421-14 CESEDA", List.of(),
                "à voir");

        when(strategicOptionRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(
                analysis.getId(), StrategicOptionStatus.RETAINED))
                .thenReturn(List.of(piste));
        when(titleDecisionRepository.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any())).thenReturn(new ArrayList<>());

        service.materializeForAnalysis(analysis);

        verify(caseDeadlineRepository, never()).save(any());
    }

    @Test
    void materializeForAnalysis_recoursAligned_byKeyword() {
        CaseAnalysis analysis = newAnalysis();
        UUID caseFileId = analysis.getCaseFile().getId();
        StrategicOption piste = newPiste("Engager un RAPO préfectoral", null, null);

        when(strategicOptionRepository.findByCaseAnalysisIdAndStatutOrderByOrdreAsc(
                analysis.getId(), StrategicOptionStatus.RETAINED))
                .thenReturn(List.of(piste));

        ImmigrationRecours recours = new ImmigrationRecours();
        recours.setRecoursType("RECOURS_GRACIEUX_PREFET");
        when(recoursRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.of(recours));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getRetainedPistesAlignmentJson())
                .contains("\"matchStatus\":\"ALIGNED\"");
    }

    @Test
    void getForLatestAnalysis_legacyAnalysisWithoutJson_returnsEmptyList() {
        // Le test n'a pas besoin de mock du flow de sécurité — on ne teste que la branche
        // "analyse legacy" : on bypass via le helper deserializeAlignment exposé.
        assertThat(service.deserializeAlignment(null)).isEmpty();
        assertThat(service.deserializeAlignment("")).isEmpty();
        assertThat(service.deserializeAlignment("[]")).isEmpty();
    }

    @Test
    void deserializeAlignment_validJson_returnsList() throws Exception {
        List<RetainedPisteAlignment> list = List.of(new RetainedPisteAlignment(
                UUID.randomUUID(), "Texte", "L.421-14", "Court terme",
                List.of("Cond"), RetainedPisteToolMatcher.TOOL_TITRE,
                RetainedPisteAlignment.STATUS_ALIGNED));
        String json = MAPPER.writeValueAsString(list);

        List<RetainedPisteAlignment> got = service.deserializeAlignment(json);

        assertThat(got).hasSize(1);
        assertThat(got.get(0).matchStatus()).isEqualTo(RetainedPisteAlignment.STATUS_ALIGNED);
    }

    @Test
    void parseHorizonMonths_variants() {
        assertThat(RetainedPisteAlignmentService.parseHorizonMonths("Court terme (3-6 mois)")).isEqualTo(6);
        assertThat(RetainedPisteAlignmentService.parseHorizonMonths("Long terme (2 ans)")).isEqualTo(24);
        assertThat(RetainedPisteAlignmentService.parseHorizonMonths("18 mois")).isEqualTo(18);
        assertThat(RetainedPisteAlignmentService.parseHorizonMonths("1 an")).isEqualTo(12);
        assertThat(RetainedPisteAlignmentService.parseHorizonMonths("à voir")).isNull();
        assertThat(RetainedPisteAlignmentService.parseHorizonMonths(null)).isNull();
    }

    // ---- helpers ----

    private CaseAnalysis newAnalysis() {
        Workspace ws = new Workspace();
        ws.setName("Test");
        CaseFile cf = new CaseFile();
        cf.setWorkspace(ws);
        CaseAnalysis a = new CaseAnalysis();
        a.setCaseFile(cf);
        // simuler ID via reflection (PrePersist non déclenché)
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

    private StrategicOption newPiste(String texte, String baseJuridique, List<String> conditions) {
        return newPiste(texte, baseJuridique, conditions, null);
    }

    private StrategicOption newPiste(String texte, String baseJuridique,
                                      List<String> conditions, String horizon) {
        StrategicOption p = new StrategicOption();
        try {
            java.lang.reflect.Field idField = StrategicOption.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(p, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        p.setTexte(texte);
        p.setBaseJuridique(baseJuridique);
        p.setHorizonTemporel(horizon);
        if (conditions != null && !conditions.isEmpty()) {
            try {
                p.setConditionsJson(MAPPER.writeValueAsString(conditions));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        p.setStatut(StrategicOptionStatus.RETAINED);
        return p;
    }
}
