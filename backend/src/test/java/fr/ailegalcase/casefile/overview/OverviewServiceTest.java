package fr.ailegalcase.casefile.overview;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AiQuestionQueryService;
import fr.ailegalcase.analysis.AiQuestionResponse;
import fr.ailegalcase.analysis.AnalysisJobRepository;
import fr.ailegalcase.analysis.PieceManquanteAlignment;
import fr.ailegalcase.analysis.PieceManquanteAlignmentService;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.AdmissibilityStatus;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileDashboardResponse;
import fr.ailegalcase.casefile.CaseFileDashboardService;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.CaseIntakeResponse;
import fr.ailegalcase.casefile.CaseIntakeService;
import fr.ailegalcase.casefile.CasePhaseResponse;
import fr.ailegalcase.casefile.CasePhaseService;
import fr.ailegalcase.casefile.CasePhaseTimelineResponse;
import fr.ailegalcase.casefile.CasePhaseType;
import fr.ailegalcase.casefile.ContradictoireParty;
import fr.ailegalcase.casefile.ContradictoireRoundResponse;
import fr.ailegalcase.casefile.ContradictoireService;
import fr.ailegalcase.casefile.ContradictoireTimelineResponse;
import fr.ailegalcase.casefile.DashboardTile;
import fr.ailegalcase.casefile.EcheancierItem;
import fr.ailegalcase.casefile.EcheancierResponse;
import fr.ailegalcase.casefile.EcheancierService;
import fr.ailegalcase.casefile.PiecesWaveResponse;
import fr.ailegalcase.casefile.PiecesWaveService;
import fr.ailegalcase.casefile.PrescriptionStatus;
import fr.ailegalcase.casefile.conclusion.CaseConclusionRepository;
import fr.ailegalcase.casefile.strategy.CaseStrategyRepository;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * F-289 / SF-289-01 — tests unitaires de l'agrégation : pilotage, tri du fil,
 * troncature de l'attention à 5, et fail-open par source.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OverviewServiceTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID WS_ID = UUID.randomUUID();

    @Mock private CaseFileRepository caseFileRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private CasePhaseService casePhaseService;
    @Mock private ContradictoireService contradictoireService;
    @Mock private EcheancierService echeancierService;
    @Mock private CaseIntakeService caseIntakeService;
    @Mock private PiecesWaveService piecesWaveService;
    @Mock private CaseFileDashboardService dashboardService;
    @Mock private PieceManquanteAlignmentService pieceManquanteAlignmentService;
    @Mock private AiQuestionQueryService aiQuestionQueryService;
    @Mock private AnalysisJobRepository analysisJobRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private CaseStrategyRepository strategyRepository;
    @Mock private CaseConclusionRepository conclusionRepository;
    @Mock private OidcUser oidcUser;
    @Mock private Principal principal;

    private OverviewService service;

    @BeforeEach
    void setUp() {
        service = new OverviewService(caseFileRepository, workspaceMemberRepository, currentUserResolver,
                casePhaseService, contradictoireService, echeancierService, caseIntakeService,
                piecesWaveService, dashboardService, pieceManquanteAlignmentService, aiQuestionQueryService,
                analysisJobRepository, documentRepository, strategyRepository, conclusionRepository,
                new ObjectMapper());

        Workspace ws = new Workspace();
        ws.setId(WS_ID);
        CaseFile cf = new CaseFile();
        cf.setId(CASE_ID);
        cf.setWorkspace(ws);

        User user = new User();
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CASE_ID)).thenReturn(Optional.of(cf));
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));

        // Defaults : tout vide (chaque test surcharge ce qu'il exerce).
        lenient().when(piecesWaveService.wave(any(), any(), any())).thenReturn(PiecesWaveResponse.none());
        lenient().when(pieceManquanteAlignmentService.getForLatestAnalysis(any(), any(), any()))
                .thenReturn(List.of());
        lenient().when(aiQuestionQueryService.listQuestions(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(analysisJobRepository.findByCaseFileIdAndJobType(any(), any())).thenReturn(Optional.empty());
        lenient().when(strategyRepository.findByCaseFileId(any())).thenReturn(Optional.empty());
        lenient().when(conclusionRepository.findByCaseFileIdOrderByVersionNumberDesc(any())).thenReturn(List.of());
    }

    @Test
    void buildsPilotageFromSources() {
        when(casePhaseService.timeline(any(), any(), any())).thenReturn(new CasePhaseTimelineResponse(
                List.of(new CasePhaseResponse(UUID.randomUUID(), CasePhaseType.MISE_EN_ETAT,
                        "Mise en état", LocalDate.now().minusDays(10), null, null, null)),
                CasePhaseType.MISE_EN_ETAT));
        when(contradictoireService.timeline(any(), any(), any())).thenReturn(new ContradictoireTimelineResponse(
                List.of(), new ContradictoireTimelineResponse.Summary(1, ContradictoireParty.OURS, null)));
        EcheancierItem next = new EcheancierItem(UUID.randomUUID(), "Conclusions à déposer",
                LocalDate.now().plusDays(3), 3, EcheancierItem.EcheancierUrgency.CRITICAL,
                EcheancierItem.EcheancierKind.DEADLINE, "MANUAL");
        when(echeancierService.forCaseFile(any(), any(), any())).thenReturn(new EcheancierResponse(
                List.of(next), next, new EcheancierResponse.Counts(0, 1, 0, 0, 1)));
        when(caseIntakeService.get(any(), any(), any())).thenReturn(new CaseIntakeResponse(
                "Licenciement", AdmissibilityStatus.RECEVABLE, PrescriptionStatus.DANS_LES_DELAIS,
                null, null, null, null, null, true));
        when(dashboardService.getDashboard(any(), any(), any())).thenReturn(new CaseFileDashboardResponse(
                CASE_ID, "DROIT_DU_TRAVAIL", 42, "MOYEN",
                "{\"niveau\":\"ELEVE\"}",
                List.of(new DashboardTile("F-DT-08", "VALIDITE", "Licenciement", "Sans cause", null, "ALERT"))));

        OverviewResponse r = service.overview(CASE_ID, oidcUser, principal);

        assertThat(r.pilotage().currentPhaseLabel()).isEqualTo("Mise en état");
        assertThat(r.pilotage().awaitingParty()).isEqualTo("OURS");
        assertThat(r.pilotage().nextDeadline().label()).isEqualTo("Conclusions à déposer");
        assertThat(r.pilotage().nextDeadline().urgency()).isEqualTo("CRITICAL");
        assertThat(r.pilotage().sante().admissibility()).isEqualTo("RECEVABLE");
        assertThat(r.pilotage().sante().prescriptionStatus()).isEqualTo("DANS_LES_DELAIS");
        assertThat(r.pilotage().sante().riskLevelAvocat()).isEqualTo("ELEVE");
        assertThat(r.pilotage().toolsCalculated()).isEqualTo(1);
        assertThat(r.pilotage().toolsVisible()).isNull();
        assertThat(r.pilotage().analysisStale()).isFalse();
    }

    @Test
    void filIsSortedByDateAscending() {
        // Une phase ancienne, un round récent → le fil doit être trié par date croissante.
        when(casePhaseService.timeline(any(), any(), any())).thenReturn(new CasePhaseTimelineResponse(
                List.of(new CasePhaseResponse(UUID.randomUUID(), CasePhaseType.MISE_EN_ETAT,
                        "Mise en état", LocalDate.of(2026, 1, 1), null, null, null)),
                CasePhaseType.MISE_EN_ETAT));
        when(contradictoireService.timeline(any(), any(), any())).thenReturn(new ContradictoireTimelineResponse(
                List.of(new ContradictoireRoundResponse(UUID.randomUUID(), 1, ContradictoireParty.ADVERSE,
                        "Conclusions adverses", LocalDate.of(2026, 3, 1), null, null, null, null, null, null)),
                new ContradictoireTimelineResponse.Summary(1, ContradictoireParty.OURS, null)));

        OverviewResponse r = service.overview(CASE_ID, oidcUser, principal);

        assertThat(r.fil()).hasSize(2);
        assertThat(r.fil().get(0).date()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(r.fil().get(1).date()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(r.fil().get(0).voie()).isEqualTo("PROCEDURE");
        assertThat(r.fil().get(1).voie()).isEqualTo("ECHANGE");
        // Round adverse → CTA de génération de réplique.
        assertThat(r.fil().get(1).action().kind()).isEqualTo("GENERATE_REPLY");
        assertThat(r.fil().get(0).temps()).isEqualTo("PASSE");
    }

    @Test
    void attentionIsTruncatedToFiveButTotalKept() {
        // 6 échéances CRITICAL → attention tronquée à 5, attentionTotal = 6.
        java.util.List<EcheancierItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            items.add(new EcheancierItem(UUID.randomUUID(), "Échéance " + i,
                    LocalDate.now().plusDays(2), 2, EcheancierItem.EcheancierUrgency.CRITICAL,
                    EcheancierItem.EcheancierKind.DEADLINE, "MANUAL"));
        }
        when(echeancierService.forCaseFile(any(), any(), any())).thenReturn(new EcheancierResponse(
                items, items.get(0), new EcheancierResponse.Counts(0, 6, 0, 0, 6)));

        OverviewResponse r = service.overview(CASE_ID, oidcUser, principal);

        assertThat(r.attention()).hasSize(5);
        assertThat(r.attentionTotal()).isEqualTo(6);
        assertThat(r.attention()).allMatch(a -> "ECHEANCE".equals(a.type()));
    }

    @Test
    void unansweredQuestionsAndPiecesAndStaleProduceAttention() {
        UUID q1Id = UUID.randomUUID();
        when(aiQuestionQueryService.listQuestions(any(), any(), any(), any())).thenReturn(List.of(
                new AiQuestionResponse(q1Id, 0, "Q1 ?", null, null, null),
                new AiQuestionResponse(UUID.randomUUID(), 1, "Q2 ?", "réponse", null, null)));
        when(pieceManquanteAlignmentService.getForLatestAnalysis(any(), any(), any())).thenReturn(List.of(
                new PieceManquanteAlignment("Contrat de travail", "A_DEMANDER", "client", null),
                new PieceManquanteAlignment("Bulletin de paie", "OBTENUE", null, null)));
        when(piecesWaveService.wave(any(), any(), any())).thenReturn(new PiecesWaveResponse(
                java.time.Instant.now(), 2, List.of()));

        OverviewResponse r = service.overview(CASE_ID, oidcUser, principal);

        assertThat(r.attention()).extracting(OverviewResponse.AttentionItem::type)
                .contains("QUESTION_IA", "PIECE_MANQUANTE", "ANALYSE_OBSOLETE");
        // 1 seule question non répondue (Q1) → libellé singulier + id porté (SF-289-02 inline).
        assertThat(r.attention()).anyMatch(a -> "QUESTION_IA".equals(a.type())
                && a.label().equals("1 question sans réponse")
                && q1Id.equals(a.questionId()));
        // SF-289-02 — la pièce A_DEMANDER porte son libellé original (clé du PUT statut).
        assertThat(r.attention()).anyMatch(a -> "PIECE_MANQUANTE".equals(a.type())
                && "Contrat de travail".equals(a.pieceLibelle()));
        assertThat(r.pilotage().analysisStale()).isTrue();
        assertThat(r.pilotage().pendingPiecesCount()).isEqualTo(2);
    }

    @Test
    void multipleUnansweredQuestions_questionIdNull_routedNotInline() {
        // SF-289-02 — ≥ 2 questions sans réponse → item agrégé sans id (l'avocat est routé).
        when(aiQuestionQueryService.listQuestions(any(), any(), any(), any())).thenReturn(List.of(
                new AiQuestionResponse(UUID.randomUUID(), 0, "Q1 ?", null, null, null),
                new AiQuestionResponse(UUID.randomUUID(), 1, "Q2 ?", null, null, null)));

        OverviewResponse r = service.overview(CASE_ID, oidcUser, principal);

        assertThat(r.attention()).anyMatch(a -> "QUESTION_IA".equals(a.type())
                && a.label().equals("2 questions sans réponse")
                && a.questionId() == null);
    }

    @Test
    void failOpen_oneSourceThrows_aggregateStillServedWithoutIt() {
        // L'échéancier jette : l'agrégat doit être servi sans sa contribution.
        when(echeancierService.forCaseFile(any(), any(), any()))
                .thenThrow(new RuntimeException("boom échéancier"));
        // Une autre source répond normalement.
        when(casePhaseService.timeline(any(), any(), any())).thenReturn(new CasePhaseTimelineResponse(
                List.of(new CasePhaseResponse(UUID.randomUUID(), CasePhaseType.MISE_EN_ETAT,
                        "Mise en état", LocalDate.now().minusDays(5), null, null, null)),
                CasePhaseType.MISE_EN_ETAT));

        OverviewResponse r = service.overview(CASE_ID, oidcUser, principal);

        // Pas d'exception : l'agrégat est servi.
        assertThat(r).isNotNull();
        assertThat(r.pilotage().currentPhaseLabel()).isEqualTo("Mise en état");
        // L'échéancier étant tombé : pas de prochain couperet, pas d'item ECHEANCE.
        assertThat(r.pilotage().nextDeadline()).isNull();
        assertThat(r.attention()).noneMatch(a -> "ECHEANCE".equals(a.type()));
    }

    // ── SF-289-03 : documents versés pendant une phase ──────────────────────────

    /** Document daté à {@code date} (00h, fuseau système → LocalDate stable). */
    private Document doc(UUID id, String filename, LocalDate date) {
        Document d = new Document();
        d.setId(id);
        d.setOriginalFilename(filename);
        d.setCreatedAt(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return d;
    }

    @Test
    void phaseEvents_attachDocumentsInTheirWindow() {
        // 3 phases : P1 (01/01), P2 (01/03), P3 (01/05, courante).
        CasePhaseResponse p1 = new CasePhaseResponse(UUID.randomUUID(), CasePhaseType.SAISINE,
                "Saisine", LocalDate.of(2026, 1, 1), null, null, null);
        CasePhaseResponse p2 = new CasePhaseResponse(UUID.randomUUID(), CasePhaseType.MISE_EN_ETAT,
                "Mise en état", LocalDate.of(2026, 3, 1), null, null, null);
        CasePhaseResponse p3 = new CasePhaseResponse(UUID.randomUUID(), CasePhaseType.FOND,
                "Jugement au fond", LocalDate.of(2026, 5, 1), null, null, null);
        // Phases volontairement non triées en entrée → le service doit les ordonner.
        when(casePhaseService.timeline(any(), any(), any())).thenReturn(new CasePhaseTimelineResponse(
                List.of(p3, p1, p2), CasePhaseType.FOND));

        UUID dA = UUID.randomUUID();   // dans P1
        UUID dB = UUID.randomUUID();   // dans P2 (à la borne basse exacte)
        UUID dC = UUID.randomUUID();   // dans P2
        UUID dD = UUID.randomUUID();   // dans P3 (courante, après aujourd'hui non testé)
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(CASE_ID)).thenReturn(List.of(
                doc(dD, "audience.pdf", LocalDate.of(2026, 6, 1)),
                doc(dC, "piece2.pdf", LocalDate.of(2026, 4, 15)),
                doc(dB, "piece1.pdf", LocalDate.of(2026, 3, 1)),
                doc(dA, "contrat.pdf", LocalDate.of(2026, 1, 10))));

        OverviewResponse r = service.overview(CASE_ID, oidcUser, principal);

        OverviewResponse.TimelineEvent e1 = filEvent(r, LocalDate.of(2026, 1, 1));
        OverviewResponse.TimelineEvent e2 = filEvent(r, LocalDate.of(2026, 3, 1));
        OverviewResponse.TimelineEvent e3 = filEvent(r, LocalDate.of(2026, 5, 1));

        // P1 = [01/01, 01/03[ → contrat seul.
        assertThat(e1.attachments()).extracting(OverviewResponse.Attachment::documentId)
                .containsExactlyInAnyOrder(dA);
        // P2 = [01/03, 01/05[ → piece1 (borne basse incluse) + piece2.
        assertThat(e2.attachments()).extracting(OverviewResponse.Attachment::documentId)
                .containsExactlyInAnyOrder(dB, dC);
        // P3 = [01/05, +∞[ → audience (postérieure).
        assertThat(e3.attachments()).extracting(OverviewResponse.Attachment::documentId)
                .containsExactlyInAnyOrder(dD);
        assertThat(e1.attachments()).first()
                .extracting(OverviewResponse.Attachment::filename).isEqualTo("contrat.pdf");
    }

    @Test
    void phaseEvents_lastPhaseCapturesDocumentsUpToNow() {
        // Une seule phase ancienne → fenêtre ouverte jusqu'à maintenant : capte tout.
        when(casePhaseService.timeline(any(), any(), any())).thenReturn(new CasePhaseTimelineResponse(
                List.of(new CasePhaseResponse(UUID.randomUUID(), CasePhaseType.SAISINE,
                        "Saisine", LocalDate.now().minusDays(30), null, null, null)),
                CasePhaseType.SAISINE));
        UUID dOld = UUID.randomUUID();
        UUID dRecent = UUID.randomUUID();
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(CASE_ID)).thenReturn(List.of(
                doc(dRecent, "recent.pdf", LocalDate.now()),
                doc(dOld, "old.pdf", LocalDate.now().minusDays(10))));

        OverviewResponse r = service.overview(CASE_ID, oidcUser, principal);

        OverviewResponse.TimelineEvent phase = filEvent(r, LocalDate.now().minusDays(30));
        assertThat(phase.attachments()).extracting(OverviewResponse.Attachment::documentId)
                .containsExactlyInAnyOrder(dOld, dRecent);
    }

    @Test
    void noPhase_behaviourUnchanged_noProcedureEvent() {
        // Aucune phase → aucun event PROCEDURE, et le repo documents n'est pas requis.
        when(casePhaseService.timeline(any(), any(), any())).thenReturn(new CasePhaseTimelineResponse(
                List.of(), null));

        OverviewResponse r = service.overview(CASE_ID, oidcUser, principal);

        assertThat(r.fil()).noneMatch(e -> "PROCEDURE".equals(e.voie()));
    }

    @Test
    void phaseEvents_documentResolutionThrows_failOpen_phaseEventsWithoutAttachments() {
        when(casePhaseService.timeline(any(), any(), any())).thenReturn(new CasePhaseTimelineResponse(
                List.of(new CasePhaseResponse(UUID.randomUUID(), CasePhaseType.MISE_EN_ETAT,
                        "Mise en état", LocalDate.of(2026, 2, 1), null, null, null)),
                CasePhaseType.MISE_EN_ETAT));
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(CASE_ID))
                .thenThrow(new RuntimeException("boom documents"));

        OverviewResponse r = service.overview(CASE_ID, oidcUser, principal);

        // Fail-open : l'agrégat reste servi, l'event de phase existe mais sans pièces.
        OverviewResponse.TimelineEvent phase = filEvent(r, LocalDate.of(2026, 2, 1));
        assertThat(phase.voie()).isEqualTo("PROCEDURE");
        assertThat(phase.attachments()).isEmpty();
    }

    private static OverviewResponse.TimelineEvent filEvent(OverviewResponse r, LocalDate date) {
        return r.fil().stream()
                .filter(e -> "PROCEDURE".equals(e.voie()) && date.equals(e.date()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aucun event PROCEDURE à la date " + date));
    }
}
