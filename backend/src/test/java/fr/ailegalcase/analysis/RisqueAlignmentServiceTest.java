package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-195 SF-195-01 — UT du service d'alignement risques (matérialisation +
 * recompute score_risque_avocat + lecture).
 *
 * <p>Pattern miroir {@link PieceManquanteAlignmentServiceTest} (F-194).
 * Différences F-195 : (a) trichotomie A_CREUSER/VALIDE/ECARTE ; (b) recompute
 * score parallèle excluant ÉCARTÉ ; (c) mapping toolIds via RisqueToolMatcher.</p>
 */
class RisqueAlignmentServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RisqueStatusRepository risqueStatusRepository;
    private CaseAnalysisRepository caseAnalysisRepository;
    private CaseFileRepository caseFileRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private CurrentUserResolver currentUserResolver;

    private RisqueAlignmentService service;

    @BeforeEach
    void setUp() {
        risqueStatusRepository = mock(RisqueStatusRepository.class);
        caseAnalysisRepository = mock(CaseAnalysisRepository.class);
        caseFileRepository = mock(CaseFileRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        currentUserResolver = mock(CurrentUserResolver.class);

        service = new RisqueAlignmentService(
                risqueStatusRepository, caseAnalysisRepository, caseFileRepository,
                workspaceMemberRepository, currentUserResolver);
    }

    @Test
    void materializeForAnalysis_emptyJson_persistsEmptyAlignment() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(null);
        when(risqueStatusRepository.findByCaseFileId(any())).thenReturn(List.of());

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getRisquesAlignmentJson()).isEqualTo("[]");
    }

    @Test
    void materializeForAnalysis_risquesIaWithoutOverlay_defaultsToACreuser() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"risques\":[{\"texte\":\"Harcèlement moral subi\"},"
                        + "{\"texte\":\"Discrimination liée à l'origine\"}]}");
        when(risqueStatusRepository.findByCaseFileId(any())).thenReturn(List.of());

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getRisquesAlignmentJson();
        assertThat(json).contains("\"risqueLibelle\":\"Harcèlement moral subi\"")
                .contains("\"risqueLibelle\":\"Discrimination liée à l'origine\"")
                .contains("\"statut\":\"A_CREUSER\"");
        // Mapping toolIds doit être présent
        assertThat(json).contains(RisqueToolMatcher.TOOL_HARCELEMENT_NULLITE);
        assertThat(json).contains(RisqueToolMatcher.TOOL_DISCRIMINATION);
    }

    @Test
    void materializeForAnalysis_overlayValide_joinsAndPreservesStatus() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"risques\":[{\"texte\":\"Harcèlement moral\"}]}");

        RisqueStatus overlay = newOverlay("harcèlement moral", "Harcèlement moral",
                RisqueStatus.STATUT_VALIDE, null);
        when(risqueStatusRepository.findByCaseFileId(any())).thenReturn(List.of(overlay));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getRisquesAlignmentJson();
        assertThat(json).contains("\"statut\":\"VALIDE\"");
    }

    @Test
    void materializeForAnalysis_overlayEcarte_joinsWithRaison() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"risques\":[{\"texte\":\"Clause non-concurrence abusive\"}]}");

        RisqueStatus overlay = newOverlay("clause non-concurrence abusive",
                "Clause non-concurrence abusive",
                RisqueStatus.STATUT_ECARTE, "Délai expiré");
        when(risqueStatusRepository.findByCaseFileId(any())).thenReturn(List.of(overlay));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getRisquesAlignmentJson();
        assertThat(json).contains("\"statut\":\"ECARTE\"")
                .contains("\"raisonEcarte\":\"Délai expiré\"");
    }

    @Test
    void materializeForAnalysis_normalizationJoinsCaseAndWhitespace() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"risques\":[{\"texte\":\"  HARCÈLEMENT moral  \"}]}");

        RisqueStatus overlay = newOverlay("harcèlement moral", "Harcèlement moral",
                RisqueStatus.STATUT_VALIDE, null);
        when(risqueStatusRepository.findByCaseFileId(any())).thenReturn(List.of(overlay));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getRisquesAlignmentJson()).contains("\"statut\":\"VALIDE\"");
    }

    @Test
    void materializeForAnalysis_risqueNotInIaButValideOverlay_addedToAlignment() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"risques\":[]}");

        RisqueStatus overlay = newOverlay("harcèlement", "Harcèlement",
                RisqueStatus.STATUT_VALIDE, null);
        when(risqueStatusRepository.findByCaseFileId(any())).thenReturn(List.of(overlay));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getRisquesAlignmentJson())
                .contains("\"risqueLibelle\":\"Harcèlement\"")
                .contains("\"statut\":\"VALIDE\"");
    }

    @Test
    void materializeForAnalysis_risqueNotInIaButACreuserOverlay_notAdded() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"risques\":[]}");

        RisqueStatus overlay = newOverlay("ancien risque", "Ancien risque",
                RisqueStatus.STATUT_A_CREUSER, null);
        when(risqueStatusRepository.findByCaseFileId(any())).thenReturn(List.of(overlay));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getRisquesAlignmentJson()).isEqualTo("[]");
    }

    @Test
    void materializeForAnalysis_doesNotMutateAnalysisResult_FIA02Strict() {
        CaseAnalysis analysis = newAnalysis();
        String initial = "{\"risques\":[{\"texte\":\"Harcèlement\"}],\"score_risque\":{\"niveau\":\"ELEVE\",\"valeur\":80}}";
        analysis.setAnalysisResult(initial);
        analysis.setRiskScore(80);
        analysis.setRiskLevel("ELEVE");
        when(risqueStatusRepository.findByCaseFileId(any())).thenReturn(List.of());

        service.materializeForAnalysis(analysis);

        // analysis_result inchangé
        assertThat(analysis.getAnalysisResult()).isEqualTo(initial);
        // riskScore IA brut inchangé (F-IA-02 strict)
        assertThat(analysis.getRiskScore()).isEqualTo(80);
        assertThat(analysis.getRiskLevel()).isEqualTo("ELEVE");
    }

    @Test
    void materializeForAnalysis_recomputesScoreAvocat_excludingEcartes() throws Exception {
        // 3 risques IA, 1 écarté → score recomputé = score brut * 2/3
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"risques\":[{\"texte\":\"R1\"},{\"texte\":\"R2\"},{\"texte\":\"R3\"}]}");
        analysis.setRiskScore(90);
        analysis.setRiskLevel("ELEVE");

        RisqueStatus s1 = newOverlay("r1", "R1", RisqueStatus.STATUT_VALIDE, null);
        RisqueStatus s2 = newOverlay("r2", "R2", RisqueStatus.STATUT_ECARTE, "raison");
        // R3 reste A_CREUSER (pas d'overlay)
        when(risqueStatusRepository.findByCaseFileId(any())).thenReturn(List.of(s1, s2));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String scoreJson = saved.getValue().getScoreRisqueAvocatJson();
        assertThat(scoreJson).isNotNull();
        JsonNode node = MAPPER.readTree(scoreJson);
        // 90 * 2/3 = 60 (MOYEN) — risquesValides=1 (R1) + risquesACreuser=1 (R3) = 2 retenus
        assertThat(node.get("valeur").asInt()).isEqualTo(60);
        assertThat(node.get("niveau").asText()).isEqualTo("MOYEN");
        assertThat(node.get("scoreIaBrut").asInt()).isEqualTo(90);
        assertThat(node.get("niveauIaBrut").asText()).isEqualTo("ELEVE");
        assertThat(node.get("totalRisques").asInt()).isEqualTo(3);
        assertThat(node.get("risquesValides").asInt()).isEqualTo(1);
        assertThat(node.get("risquesEcartes").asInt()).isEqualTo(1);
        assertThat(node.get("risquesACreuser").asInt()).isEqualTo(1);
    }

    @Test
    void materializeForAnalysis_allEcartes_scoreAvocatZero() throws Exception {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"risques\":[{\"texte\":\"R1\"}]}");
        analysis.setRiskScore(80);
        analysis.setRiskLevel("ELEVE");

        RisqueStatus s1 = newOverlay("r1", "R1", RisqueStatus.STATUT_ECARTE, null);
        when(risqueStatusRepository.findByCaseFileId(any())).thenReturn(List.of(s1));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        JsonNode node = MAPPER.readTree(saved.getValue().getScoreRisqueAvocatJson());
        assertThat(node.get("valeur").asInt()).isEqualTo(0);
        assertThat(node.get("niveau").asText()).isEqualTo("FAIBLE");
    }

    @Test
    void materializeForAnalysis_noEcartes_scoreAvocatEqualsScoreBrut() throws Exception {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"risques\":[{\"texte\":\"R1\"}]}");
        analysis.setRiskScore(70);
        analysis.setRiskLevel("ELEVE");

        when(risqueStatusRepository.findByCaseFileId(any())).thenReturn(List.of());

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        JsonNode node = MAPPER.readTree(saved.getValue().getScoreRisqueAvocatJson());
        assertThat(node.get("valeur").asInt()).isEqualTo(70);
    }

    @Test
    void materializeForAnalysis_idempotent_secondRunSamePayload() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"risques\":[{\"texte\":\"R1\"}]}");
        analysis.setRiskScore(50);
        when(risqueStatusRepository.findByCaseFileId(any())).thenReturn(List.of());

        service.materializeForAnalysis(analysis);
        String json1 = analysis.getRisquesAlignmentJson();
        String score1 = analysis.getScoreRisqueAvocatJson();

        // 2e run sur la même analyse — alignement et score identiques
        service.materializeForAnalysis(analysis);
        String json2 = analysis.getRisquesAlignmentJson();
        String score2 = analysis.getScoreRisqueAvocatJson();

        assertThat(json1).isEqualTo(json2);
        assertThat(score1).isEqualTo(score2);
    }

    @Test
    void materializeForAnalysis_repoThrows_failsOpen() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"risques\":[]}");
        when(risqueStatusRepository.findByCaseFileId(any()))
                .thenThrow(new RuntimeException("DB down"));

        // Ne doit pas lever — fail-open
        service.materializeForAnalysis(analysis);

        // analysis_result intact
        assertThat(analysis.getAnalysisResult()).isEqualTo("{\"risques\":[]}");
        // Aucun save (l'extraction a échoué avant)
        verify(caseAnalysisRepository, never()).save(any());
    }

    @Test
    void materializeForAnalysis_dedupesIdenticalRisquesIa() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"risques\":[{\"texte\":\"Harcèlement\"},{\"texte\":\"harcèlement\"}]}");
        when(risqueStatusRepository.findByCaseFileId(any())).thenReturn(List.of());

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getRisquesAlignmentJson();
        long count = json.split("\"risqueLibelle\"", -1).length - 1;
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void deserializeAlignment_validJson_returnsList() throws Exception {
        List<RisqueAlignment> list = List.of(
                new RisqueAlignment("Harcèlement", "VALIDE", null, List.of("F-DT-12-harcelement-licenciement-nul")),
                new RisqueAlignment("Discrimination", "A_CREUSER", null, List.of()));
        String json = MAPPER.writeValueAsString(list);

        List<RisqueAlignment> got = service.deserializeAlignment(json);

        assertThat(got).hasSize(2);
        assertThat(got.get(0).statut()).isEqualTo("VALIDE");
        assertThat(got.get(1).statut()).isEqualTo("A_CREUSER");
    }

    @Test
    void deserializeAlignment_nullOrEmpty_returnsEmpty() {
        assertThat(service.deserializeAlignment(null)).isEmpty();
        assertThat(service.deserializeAlignment("")).isEmpty();
        assertThat(service.deserializeAlignment("   ")).isEmpty();
    }

    @Test
    void deserializeAlignment_invalidJson_returnsEmpty() {
        assertThat(service.deserializeAlignment("not-json")).isEmpty();
    }

    @Test
    void extractRisquesFromAnalysisResult_legacyStringFormat_parsedToo() {
        List<String> risques = RisqueAlignmentService
                .extractRisquesFromAnalysisResult("{\"risques\":[\"R1\",\"R2\"]}");
        assertThat(risques).containsExactly("R1", "R2");
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

    private RisqueStatus newOverlay(String norm, String original,
                                     String statut, String raison) {
        RisqueStatus s = new RisqueStatus();
        s.setRisqueLibelleNormalise(norm);
        s.setRisqueLibelleOriginal(original);
        s.setStatut(statut);
        s.setRaisonEcarte(raison);
        return s;
    }
}
