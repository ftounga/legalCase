package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.CaseDeadline;
import fr.ailegalcase.casefile.CaseDeadlineRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.ExpectedPiece;
import fr.ailegalcase.referential.LegalReferentialService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-194 SF-194-01 — UT du service d'alignement (matérialisation + lecture).
 *
 * <p>Pattern miroir {@link RetainedPisteAlignmentServiceTest} (F-192) +
 * {@link ProcedureCheckAlignmentServiceTest} (F-193). Différence : F-194 ne
 * modifie pas le JSON {@code analysis_result.pieces_manquantes} (cohérence
 * F-92 stricte).</p>
 */
class PieceManquanteAlignmentServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 5, 6);

    private PieceManquanteStatusRepository pieceManquanteStatusRepository;
    private CaseAnalysisRepository caseAnalysisRepository;
    private CaseFileRepository caseFileRepository;
    private CaseDeadlineRepository caseDeadlineRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private CurrentUserResolver currentUserResolver;
    private LegalReferentialService legalReferentialService;
    private Clock fixedClock;

    private PieceManquanteAlignmentService service;

    @BeforeEach
    void setUp() {
        pieceManquanteStatusRepository = mock(PieceManquanteStatusRepository.class);
        caseAnalysisRepository = mock(CaseAnalysisRepository.class);
        caseFileRepository = mock(CaseFileRepository.class);
        caseDeadlineRepository = mock(CaseDeadlineRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        currentUserResolver = mock(CurrentUserResolver.class);
        legalReferentialService = mock(LegalReferentialService.class);
        fixedClock = Clock.fixed(FIXED_DATE.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

        // Par défaut : aucun socle → canonisation no-op (les tests F-194 existants
        // restent inchangés). Les tests F-294 stubbent explicitement le socle.
        when(legalReferentialService.getExpectedPieces(any(), any(), any())).thenReturn(List.of());

        service = new PieceManquanteAlignmentService(
                pieceManquanteStatusRepository, caseAnalysisRepository, caseFileRepository,
                caseDeadlineRepository, workspaceMemberRepository, currentUserResolver,
                legalReferentialService, fixedClock);
    }

    @Test
    void materializeForAnalysis_emptyJson_persistsEmptyAlignment() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(null);
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of());

        service.materializeForAnalysis(analysis);

        // Aucune pièce IA + aucun overlay → alignement vide mais persisté tout de même
        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getPiecesAlignmentJson()).isEqualTo("[]");
        verify(caseDeadlineRepository, never()).save(any());
    }

    @Test
    void materializeForAnalysis_piecesIaWithoutOverlay_defaultsToADemander() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"pieces_manquantes\":[{\"texte\":\"Contrat de travail original\"},"
                        + "{\"texte\":\"Lettre de licenciement\"}]}");
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of());
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any()))
                .thenReturn(new ArrayList<>());

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getPiecesAlignmentJson();
        assertThat(json).contains("\"pieceLibelle\":\"Contrat de travail original\"")
                .contains("\"pieceLibelle\":\"Lettre de licenciement\"")
                .contains("\"statut\":\"A_DEMANDER\"");

        // 2 délais auto créés (1 par pièce A_DEMANDER)
        ArgumentCaptor<CaseDeadline> deadlineCaptor = ArgumentCaptor.forClass(CaseDeadline.class);
        verify(caseDeadlineRepository, times(2)).save(deadlineCaptor.capture());
        for (CaseDeadline d : deadlineCaptor.getAllValues()) {
            assertThat(d.getSource()).isEqualTo("PIECE_A_DEMANDER");
            assertThat(d.getDueDate()).isEqualTo(FIXED_DATE.plusDays(14));
            assertThat(d.getLabel()).startsWith("Demander au client : ");
        }
    }

    @Test
    void materializeForAnalysis_overlayObtenue_joinsAndSkipsDeadline() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"pieces_manquantes\":[{\"texte\":\"Contrat de travail original\"}]}");

        PieceManquanteStatus overlay = newOverlay("contrat de travail original",
                "Contrat de travail original",
                PieceManquanteStatus.STATUT_OBTENUE, null, "client");
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of(overlay));
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any()))
                .thenReturn(new ArrayList<>());

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getPiecesAlignmentJson();
        assertThat(json).contains("\"statut\":\"OBTENUE\"")
                .contains("\"destinataire\":\"client\"");

        // Pas de délai auto pour OBTENUE
        verify(caseDeadlineRepository, never()).save(any());
    }

    // ── SF-194-04 PR3 — matching par clé canonique (survie au drift de libellé) ──

    @Test
    void materializeForAnalysis_canonicalKeyMatchesAcrossLabelDrift_noDuplicate() {
        // Socle F-294 : "Lettre de licenciement" est canonique.
        when(legalReferentialService.getExpectedPieces(any(), any(), any())).thenReturn(List.of(
                new fr.ailegalcase.casefile.ExpectedPiece(
                        "DT_LETTRE_LIC", "Lettre de licenciement", "FR", List.of(), true, 1)));

        // Run 2 : l'IA produit le libellé canonique du socle.
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"pieces_manquantes\":[{\"texte\":\"Lettre de licenciement\"}]}");

        // Statut posé au run 1 sur une VARIANTE de libellé (clé brute différente),
        // mais avec le libellé canonique capturé (PR3).
        PieceManquanteStatus overlay = newOverlay("lettre de licenciement (version initiale)",
                "Lettre de licenciement (version initiale)",
                PieceManquanteStatus.STATUT_OBTENUE, null, null);
        overlay.setId(UUID.randomUUID());
        overlay.setPieceLibelleCanonique("Lettre de licenciement");
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of(overlay));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getPiecesAlignmentJson();
        // CA2 : le statut OBTENUE est retrouvé via la clé canonique (pièce non réapparue A_DEMANDER).
        assertThat(json).contains("\"statut\":\"OBTENUE\"");
        // CA5 : une seule entrée (le statut consommé via canonique n'est pas re-ajouté en doublon).
        assertThat(json.split("\"pieceLibelle\"", -1).length - 1).isEqualTo(1);
        // Pas de délai auto pour OBTENUE.
        verify(caseDeadlineRepository, never()).save(any());
    }

    @Test
    void materializeForAnalysis_canonicalNull_fallsBackToRawMatching() {
        // Régression : sans canonique (lignes anciennes), le matching brut historique
        // reste inchangé (pièce OBTENUE conservée par la boucle de repli).
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");

        PieceManquanteStatus overlay = newOverlay("contrat ancien", "Contrat ancien",
                PieceManquanteStatus.STATUT_OBTENUE, null, null);
        overlay.setId(UUID.randomUUID());
        // canonique = null (non renseigné)
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of(overlay));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getPiecesAlignmentJson())
                .contains("\"pieceLibelle\":\"Contrat ancien\"")
                .contains("\"statut\":\"OBTENUE\"");
    }

    @Test
    void materializeForAnalysis_overlayNonApplicable_joinsAndSkipsDeadline() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"pieces_manquantes\":[{\"texte\":\"Convention BTP\"}]}");

        PieceManquanteStatus overlay = newOverlay("convention btp", "Convention BTP",
                PieceManquanteStatus.STATUT_NON_APPLICABLE,
                "Hors champ d'application", null);
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of(overlay));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getPiecesAlignmentJson();
        assertThat(json).contains("\"statut\":\"NON_APPLICABLE\"")
                .contains("\"raisonNonApp\":\"Hors champ d'application\"");

        verify(caseDeadlineRepository, never()).save(any());
    }

    @Test
    void materializeForAnalysis_normalizationJoinsCaseAndWhitespace() {
        // L'IA produit "  CONTRAT de Travail original  " mais l'overlay statut a été
        // posé sur "contrat de travail original" — joindre malgré différence de casse
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"pieces_manquantes\":[{\"texte\":\"  CONTRAT de Travail original  \"}]}");

        PieceManquanteStatus overlay = newOverlay("contrat de travail original",
                "Contrat de travail original",
                PieceManquanteStatus.STATUT_OBTENUE, null, null);
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of(overlay));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getPiecesAlignmentJson()).contains("\"statut\":\"OBTENUE\"");
    }

    @Test
    void materializeForAnalysis_piecesNotInIaButObtenueOverlay_addedToAlignment() {
        // L'IA a régénéré sa liste sans inclure une pièce que l'avocat avait marquée OBTENUE.
        // L'overlay doit être conservé pour mémoire dashboard.
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");

        PieceManquanteStatus overlay = newOverlay("contrat ancien",
                "Contrat ancien",
                PieceManquanteStatus.STATUT_OBTENUE, null, "client");
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of(overlay));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getPiecesAlignmentJson())
                .contains("\"pieceLibelle\":\"Contrat ancien\"")
                .contains("\"statut\":\"OBTENUE\"");
    }

    @Test
    void materializeForAnalysis_piecesNotInIaButADemanderOverlay_notAdded() {
        // L'overlay A_DEMANDER pour une pièce que l'IA ne mentionne plus :
        // V1 = pas d'ajout artificiel (seulement statuts curés OBTENUE/NON_APPLICABLE)
        // sinon on inverserait le défaut implicite.
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");

        PieceManquanteStatus overlay = newOverlay("contrat ancien",
                "Contrat ancien",
                PieceManquanteStatus.STATUT_A_DEMANDER, null, null);
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of(overlay));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        // Alignement vide (pas de pièce IA, l'overlay A_DEMANDER tout seul ne crée pas)
        assertThat(saved.getValue().getPiecesAlignmentJson()).isEqualTo("[]");
    }

    @Test
    void materializeForAnalysis_doesNotMutateAnalysisResult_F92Strict() {
        // Cohérence F-92 STRICTE : le JSON pieces_manquantes n'est PAS modifié
        CaseAnalysis analysis = newAnalysis();
        String initial = "{\"pieces_manquantes\":[{\"texte\":\"Contrat\"}]}";
        analysis.setAnalysisResult(initial);
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of());

        service.materializeForAnalysis(analysis);

        // analysis_result inchangé
        assertThat(analysis.getAnalysisResult()).isEqualTo(initial);
    }

    @Test
    void materializeForAnalysis_deadlinesIdempotent_noDuplicate() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"pieces_manquantes\":[{\"texte\":\"Contrat de travail\"}]}");
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of());

        // 1er run : aucun délai existant
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any()))
                .thenReturn(new ArrayList<>());
        service.materializeForAnalysis(analysis);
        verify(caseDeadlineRepository, times(1)).save(any());

        // 2e run : on simule que le délai créé est en base
        CaseDeadline existing = new CaseDeadline();
        existing.setSource("PIECE_A_DEMANDER");
        existing.setLabel("Demander au client : Contrat de travail");
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any()))
                .thenReturn(List.of(existing));
        service.materializeForAnalysis(analysis);
        // total reste à 1 — pas de doublon
        verify(caseDeadlineRepository, times(1)).save(any());
    }

    @Test
    void materializeForAnalysis_repoThrows_failsOpen() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");
        when(pieceManquanteStatusRepository.findByCaseFileId(any()))
                .thenThrow(new RuntimeException("DB down"));

        // Ne doit pas lever — fail-open
        service.materializeForAnalysis(analysis);

        // analysis_result intact
        assertThat(analysis.getAnalysisResult()).isEqualTo("{\"pieces_manquantes\":[]}");
        // Aucun save (l'extraction a échoué avant)
        verify(caseAnalysisRepository, never()).save(any());
        verify(caseDeadlineRepository, never()).save(any());
    }

    @Test
    void materializeForAnalysis_dedupesIdenticalPiecesIa() {
        // L'IA peut produire 2 entrées identiques (cas rare mais possible)
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"pieces_manquantes\":[{\"texte\":\"Contrat\"},{\"texte\":\"contrat\"}]}");
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of());
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any()))
                .thenReturn(new ArrayList<>());

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        // Une seule occurrence (dédup sur libellé normalisé)
        String json = saved.getValue().getPiecesAlignmentJson();
        long count = json.split("\"pieceLibelle\"", -1).length - 1;
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void deserializeAlignment_validJson_returnsList() throws Exception {
        List<PieceManquanteAlignment> list = List.of(
                new PieceManquanteAlignment("Contrat", "OBTENUE", "client", null, null),
                new PieceManquanteAlignment("Lettre", "A_DEMANDER", null, null, null));
        String json = MAPPER.writeValueAsString(list);

        List<PieceManquanteAlignment> got = service.deserializeAlignment(json);

        assertThat(got).hasSize(2);
        assertThat(got.get(0).statut()).isEqualTo("OBTENUE");
        assertThat(got.get(1).statut()).isEqualTo("A_DEMANDER");
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
    void extractPiecesFromAnalysisResult_legacyStringFormat_parsedToo() {
        // Format legacy : string direct dans le tableau (rétrocompat F-92)
        List<String> pieces = PieceManquanteAlignmentService
                .extractPiecesFromAnalysisResult("{\"pieces_manquantes\":[\"Contrat\",\"Lettre\"]}");
        assertThat(pieces).containsExactly("Contrat", "Lettre");
    }

    // ==================================================================
    //  F-294 SF-294-01 — canonisation des libellés
    // ==================================================================

    @Test
    void materializeForAnalysis_f294_canonizesLlmLabelToCanonical_CA10() {
        // Le LLM produit un libellé dont la forme normalisée correspond EXACTEMENT
        // au socle ("lettre de licenciement") → remplacé par le libellé canonique.
        CaseAnalysis analysis = newTravailFrAnalysis();
        analysis.setAnalysisResult(
                "{\"pieces_manquantes\":[{\"texte\":\"  Lettre De Licenciement  \"}]}");
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of());
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any()))
                .thenReturn(new ArrayList<>());
        stubSocle(
                new ExpectedPiece("LETTRE_LICENCIEMENT", "Lettre de licenciement", "FRANCE",
                        List.of("FOND"), true, 3));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getPiecesAlignmentJson();
        // libellé canonique substitué (pas la forme LLM avec espaces/casse)
        assertThat(json).contains("\"pieceLibelle\":\"Lettre de licenciement\"")
                .doesNotContain("Lettre De Licenciement");
    }

    @Test
    void materializeForAnalysis_f294_pieceOutsideSocle_keepsLlmLabel_CA9() {
        // Pièce hors socle (cas d'espèce) → libellé LLM conservé tel quel, jamais retirée.
        CaseAnalysis analysis = newTravailFrAnalysis();
        analysis.setAnalysisResult(
                "{\"pieces_manquantes\":[{\"texte\":\"Lettre de licenciement\"},"
                        + "{\"texte\":\"Attestation témoin spécifique\"}]}");
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of());
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any()))
                .thenReturn(new ArrayList<>());
        stubSocle(
                new ExpectedPiece("LETTRE_LICENCIEMENT", "Lettre de licenciement", "FRANCE",
                        List.of("FOND"), true, 3));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getPiecesAlignmentJson();
        // les DEUX pièces présentes : canonisée + hors-socle conservée (CA9)
        assertThat(json).contains("\"pieceLibelle\":\"Lettre de licenciement\"")
                .contains("\"pieceLibelle\":\"Attestation témoin spécifique\"");
    }

    @Test
    void materializeForAnalysis_f294_obtenueStatusPreservedAcrossTwoRuns_CA1() {
        // 1er run : l'avocat a marqué OBTENUE sur le libellé canonique. 2e run : le
        // LLM régénère une variante de casse ("LETTRE DE LICENCIEMENT") → canonisée
        // → même clé normalisée → le statut OBTENUE est préservé (pas de A_DEMANDER).
        CaseAnalysis analysis = newTravailFrAnalysis();
        analysis.setAnalysisResult(
                "{\"pieces_manquantes\":[{\"texte\":\"LETTRE DE LICENCIEMENT\"}]}");
        PieceManquanteStatus overlay = newOverlay("lettre de licenciement",
                "Lettre de licenciement",
                PieceManquanteStatus.STATUT_OBTENUE, null, "client");
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of(overlay));
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any()))
                .thenReturn(new ArrayList<>());
        stubSocle(
                new ExpectedPiece("LETTRE_LICENCIEMENT", "Lettre de licenciement", "FRANCE",
                        List.of("FOND"), true, 3));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getPiecesAlignmentJson();
        assertThat(json).contains("\"statut\":\"OBTENUE\"")
                .doesNotContain("\"statut\":\"A_DEMANDER\"");
        // pas de délai auto recréé pour une pièce OBTENUE
        verify(caseDeadlineRepository, never()).save(any());
    }

    @Test
    void materializeForAnalysis_f294_distinctLabelsNotMerged_CA10() {
        // Deux libellés distincts (3 mois vs 12 mois) ne sont JAMAIS fusionnés :
        // seul "12 derniers mois" matche le socle, l'autre garde son libellé.
        CaseAnalysis analysis = newTravailFrAnalysis();
        analysis.setAnalysisResult(
                "{\"pieces_manquantes\":[{\"texte\":\"Bulletins de paie des 3 derniers mois\"},"
                        + "{\"texte\":\"Bulletins de paie des 12 derniers mois\"}]}");
        when(pieceManquanteStatusRepository.findByCaseFileId(any())).thenReturn(List.of());
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(any()))
                .thenReturn(new ArrayList<>());
        stubSocle(
                new ExpectedPiece("BULLETINS_PAIE_12M", "Bulletins de paie des 12 derniers mois",
                        "FRANCE", List.of("FOND"), true, 2));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        String json = saved.getValue().getPiecesAlignmentJson();
        assertThat(json).contains("Bulletins de paie des 3 derniers mois")
                .contains("Bulletins de paie des 12 derniers mois");
        // 2 pièces distinctes conservées (aucune fusion)
        long count = json.split("\"pieceLibelle\"", -1).length - 1;
        assertThat(count).isEqualTo(2L);
    }

    // ---- helpers ----

    private void stubSocle(ExpectedPiece... pieces) {
        when(legalReferentialService.getExpectedPieces(any(), any(), any()))
                .thenReturn(List.of(pieces));
    }

    /** Analyse rattachée à un workspace Travail FR + stade FOND (socle applicable). */
    private CaseAnalysis newTravailFrAnalysis() {
        CaseAnalysis a = newAnalysis();
        Workspace ws = a.getCaseFile().getWorkspace();
        ws.setLegalDomain("DROIT_DU_TRAVAIL");
        ws.setCountry("FRANCE");
        a.getCaseFile().setProcedureStage("FOND");
        return a;
    }

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

    private PieceManquanteStatus newOverlay(String norm, String original,
                                             String statut, String raison, String destinataire) {
        PieceManquanteStatus s = new PieceManquanteStatus();
        s.setPieceLibelleNormalise(norm);
        s.setPieceLibelleOriginal(original);
        s.setStatut(statut);
        s.setRaisonNonApp(raison);
        s.setDestinataire(destinataire);
        return s;
    }
}
