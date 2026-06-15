package fr.ailegalcase.casefile.overview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AiQuestionQueryService;
import fr.ailegalcase.analysis.AiQuestionResponse;
import fr.ailegalcase.analysis.AnalysisJob;
import fr.ailegalcase.analysis.AnalysisJobRepository;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.analysis.PieceManquanteAlignment;
import fr.ailegalcase.analysis.PieceManquanteAlignmentService;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileDashboardResponse;
import fr.ailegalcase.casefile.CaseFileDashboardService;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.CaseIntakeResponse;
import fr.ailegalcase.casefile.CaseIntakeService;
import fr.ailegalcase.casefile.CasePhaseResponse;
import fr.ailegalcase.casefile.CasePhaseService;
import fr.ailegalcase.casefile.CasePhaseTimelineResponse;
import fr.ailegalcase.casefile.ContradictoireRoundResponse;
import fr.ailegalcase.casefile.ContradictoireService;
import fr.ailegalcase.casefile.ContradictoireTimelineResponse;
import fr.ailegalcase.casefile.DashboardTile;
import fr.ailegalcase.casefile.EcheancierItem;
import fr.ailegalcase.casefile.EcheancierResponse;
import fr.ailegalcase.casefile.EcheancierService;
import fr.ailegalcase.casefile.PiecesWaveResponse;
import fr.ailegalcase.casefile.PiecesWaveService;
import fr.ailegalcase.casefile.conclusion.CaseConclusion;
import fr.ailegalcase.casefile.conclusion.CaseConclusionRepository;
import fr.ailegalcase.casefile.overview.OverviewResponse.NextDeadline;
import fr.ailegalcase.casefile.overview.OverviewResponse.Pilotage;
import fr.ailegalcase.casefile.overview.OverviewResponse.Sante;
import fr.ailegalcase.casefile.strategy.CaseStrategy;
import fr.ailegalcase.casefile.strategy.CaseStrategyRepository;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * F-289 / SF-289-01 — agrège l'état d'un dossier en une « Vue d'ensemble »
 * (pilotage + attention + fil chronologique).
 *
 * <p><b>Lecture seule.</b> Aucune écriture, aucune nouvelle table, aucun appel LLM.
 * Fan-out {@code fail-open} <b>par source</b> (mirror {@link CaseFileDashboardService#assembleTiles})
 * : si un contributeur jette, sa contribution est omise et l'agrégat reste servi.</p>
 *
 * <p>L'isolation workspace est résolue <b>une fois</b> au début (mirror
 * {@link ContradictoireService}) : un dossier d'un autre workspace → 404. Les sources
 * lues directement via repository utilisent l'id du dossier déjà isolé.</p>
 */
@Service
public class OverviewService {

    private static final Logger log = LoggerFactory.getLogger(OverviewService.class);

    private static final String STATUT_A_DEMANDER = "A_DEMANDER";

    /** Ordre d'urgence décroissant pour le tri de la liste d'attention. */
    private static final Map<String, Integer> URGENCY_RANK = Map.of(
            "OVERDUE", 0, "CRITICAL", 1, "SOON", 2, "UPCOMING", 3, "INFO", 4);

    private static final int ATTENTION_LIMIT = 5;

    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    private final CasePhaseService casePhaseService;
    private final ContradictoireService contradictoireService;
    private final EcheancierService echeancierService;
    private final CaseIntakeService caseIntakeService;
    private final PiecesWaveService piecesWaveService;
    private final CaseFileDashboardService dashboardService;
    private final PieceManquanteAlignmentService pieceManquanteAlignmentService;
    private final AiQuestionQueryService aiQuestionQueryService;

    private final AnalysisJobRepository analysisJobRepository;
    private final DocumentRepository documentRepository;
    private final CaseStrategyRepository strategyRepository;
    private final CaseConclusionRepository conclusionRepository;
    private final ObjectMapper objectMapper;

    public OverviewService(CaseFileRepository caseFileRepository,
                           WorkspaceMemberRepository workspaceMemberRepository,
                           CurrentUserResolver currentUserResolver,
                           CasePhaseService casePhaseService,
                           ContradictoireService contradictoireService,
                           EcheancierService echeancierService,
                           CaseIntakeService caseIntakeService,
                           PiecesWaveService piecesWaveService,
                           CaseFileDashboardService dashboardService,
                           PieceManquanteAlignmentService pieceManquanteAlignmentService,
                           AiQuestionQueryService aiQuestionQueryService,
                           AnalysisJobRepository analysisJobRepository,
                           DocumentRepository documentRepository,
                           CaseStrategyRepository strategyRepository,
                           CaseConclusionRepository conclusionRepository,
                           ObjectMapper objectMapper) {
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.casePhaseService = casePhaseService;
        this.contradictoireService = contradictoireService;
        this.echeancierService = echeancierService;
        this.caseIntakeService = caseIntakeService;
        this.piecesWaveService = piecesWaveService;
        this.dashboardService = dashboardService;
        this.pieceManquanteAlignmentService = pieceManquanteAlignmentService;
        this.aiQuestionQueryService = aiQuestionQueryService;
        this.analysisJobRepository = analysisJobRepository;
        this.documentRepository = documentRepository;
        this.strategyRepository = strategyRepository;
        this.conclusionRepository = conclusionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public OverviewResponse overview(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, resolveUser(oidcUser, principal));
        LocalDate today = LocalDate.now();

        // ── Sources lues une fois (chacune fail-open) ─────────────────────────
        CasePhaseTimelineResponse phases = safe(
                () -> casePhaseService.timeline(caseFileId, oidcUser, principal), null);
        ContradictoireTimelineResponse contradictoire = safe(
                () -> contradictoireService.timeline(caseFileId, oidcUser, principal), null);
        EcheancierResponse echeancier = safe(
                () -> echeancierService.forCaseFile(caseFileId, oidcUser, principal), null);
        CaseIntakeResponse intake = safe(
                () -> caseIntakeService.get(caseFileId, oidcUser, principal), null);
        PiecesWaveResponse wave = safe(
                () -> piecesWaveService.wave(caseFileId, oidcUser, principal), PiecesWaveResponse.none());
        CaseFileDashboardResponse dashboard = safe(
                () -> dashboardService.getDashboard(caseFileId, oidcUser, principal), null);
        List<PieceManquanteAlignment> pieces = safe(
                () -> pieceManquanteAlignmentService.getForLatestAnalysis(caseFileId, oidcUser, principal),
                List.of());
        List<AiQuestionResponse> questions = safe(
                () -> aiQuestionQueryService.listQuestions(
                        caseFileId, oidcUser, OAuthProviderResolver.resolve(principal), principal),
                List.of());

        int pendingPiecesCount = wave != null ? wave.pendingCount() : 0;
        boolean analysisStale = pendingPiecesCount > 0;

        Pilotage pilotage = buildPilotage(
                phases, contradictoire, echeancier, intake, dashboard, analysisStale, pendingPiecesCount, today);

        List<OverviewResponse.AttentionItem> allAttention =
                buildAttention(echeancier, pieces, questions, analysisStale, pendingPiecesCount);
        int attentionTotal = allAttention.size();
        List<OverviewResponse.AttentionItem> attention =
                allAttention.size() > ATTENTION_LIMIT ? allAttention.subList(0, ATTENTION_LIMIT) : allAttention;

        List<OverviewResponse.TimelineEvent> fil = buildFil(
                caseFile.getId(), phases, contradictoire, wave, dashboard, echeancier, today);

        return new OverviewResponse(pilotage, attention, attentionTotal, fil);
    }

    // ── ① Pilotage ─────────────────────────────────────────────────────────────

    private Pilotage buildPilotage(CasePhaseTimelineResponse phases,
                                   ContradictoireTimelineResponse contradictoire,
                                   EcheancierResponse echeancier,
                                   CaseIntakeResponse intake,
                                   CaseFileDashboardResponse dashboard,
                                   boolean analysisStale,
                                   int pendingPiecesCount,
                                   LocalDate today) {
        String currentPhaseLabel = currentPhaseLabel(phases);
        String awaitingParty = contradictoire != null && contradictoire.summary() != null
                && contradictoire.summary().awaitingParty() != null
                ? contradictoire.summary().awaitingParty().name()
                : null;
        NextDeadline nextDeadline = nextDeadline(echeancier);
        Sante sante = buildSante(intake, dashboard);
        Integer toolsCalculated = dashboard != null && dashboard.tiles() != null
                ? dashboard.tiles().size() : null;
        // toolsVisible : non trivial à dériver côté backend en V1 → null (cf. mini-spec).
        return new Pilotage(currentPhaseLabel, awaitingParty, nextDeadline, sante,
                analysisStale, pendingPiecesCount, toolsCalculated, null);
    }

    private String currentPhaseLabel(CasePhaseTimelineResponse phases) {
        if (phases == null || phases.currentPhase() == null || phases.phases() == null) {
            return null;
        }
        // Le libellé de la phase courante = celui de la dernière transition (la plus récente).
        return phases.phases().stream()
                .filter(p -> p.phase() == phases.currentPhase())
                .reduce((first, second) -> second)
                .map(CasePhaseResponse::label)
                .orElse(phases.currentPhase().name());
    }

    private NextDeadline nextDeadline(EcheancierResponse echeancier) {
        if (echeancier == null || echeancier.nextItem() == null) {
            return null;
        }
        EcheancierItem n = echeancier.nextItem();
        return new NextDeadline(n.label(), n.dueDate(), n.daysUntil(),
                n.urgency() != null ? n.urgency().name() : null);
    }

    private Sante buildSante(CaseIntakeResponse intake, CaseFileDashboardResponse dashboard) {
        String admissibility = intake != null && intake.admissibility() != null
                ? intake.admissibility().name() : null;
        String prescription = intake != null && intake.prescriptionStatus() != null
                ? intake.prescriptionStatus().name() : null;
        String riskLevelAvocat = riskLevelAvocat(dashboard);
        return new Sante(admissibility, prescription, riskLevelAvocat);
    }

    /** Parse {@code scoreRisqueAvocatJson} → champ {@code niveau} (fail-open). */
    private String riskLevelAvocat(CaseFileDashboardResponse dashboard) {
        if (dashboard == null) {
            return null;
        }
        String json = dashboard.scoreRisqueAvocatJson();
        if (json != null && !json.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(json);
                JsonNode niveau = node.get("niveau");
                if (niveau != null && !niveau.isNull()) {
                    return niveau.asText();
                }
            } catch (Exception e) {
                log.warn("F-289 — parse scoreRisqueAvocatJson échoué : {}", e.getMessage());
            }
        }
        // Repli sur le niveau IA brut si pas de score avocat parsable.
        return dashboard.riskLevel();
    }

    // ── ② Attention ──────────────────────────────────────────────────────────

    private List<OverviewResponse.AttentionItem> buildAttention(EcheancierResponse echeancier,
                                                                List<PieceManquanteAlignment> pieces,
                                                                List<AiQuestionResponse> questions,
                                                                boolean analysisStale,
                                                                int pendingPiecesCount) {
        List<OverviewResponse.AttentionItem> items = new ArrayList<>();

        // Échéances pressantes (OVERDUE / CRITICAL).
        if (echeancier != null && echeancier.items() != null) {
            for (EcheancierItem e : echeancier.items()) {
                String urgency = e.urgency() != null ? e.urgency().name() : null;
                if ("OVERDUE".equals(urgency) || "CRITICAL".equals(urgency)) {
                    items.add(new OverviewResponse.AttentionItem(
                            "ECHEANCE", e.label(), urgency,
                            new OverviewResponse.Action("VALIDATE_DEADLINE", "suivi", "echeancier", null),
                            null, null));
                }
            }
        }

        // Pièces à demander (statut A_DEMANDER).
        if (pieces != null) {
            for (PieceManquanteAlignment p : pieces) {
                if (STATUT_A_DEMANDER.equals(p.statut())) {
                    items.add(new OverviewResponse.AttentionItem(
                            "PIECE_MANQUANTE",
                            "Pièce à obtenir : " + safeLabel(p.pieceLibelle()),
                            "SOON",
                            new OverviewResponse.Action("MARK_PIECE", "analyse", "pieces-manquantes", null),
                            null, p.pieceLibelle()));
                }
            }
        }

        // Questions IA sans réponse → 1 item agrégé.
        List<AiQuestionResponse> unansweredQuestions = questions == null ? List.of()
                : questions.stream().filter(q -> isBlank(q.answerText())).toList();
        long unanswered = unansweredQuestions.size();
        if (unanswered > 0) {
            // SF-289-02 — une seule question sans réponse → on porte son id pour
            // permettre la réponse inline ; plusieurs → questionId null (routage).
            UUID questionId = unanswered == 1 ? unansweredQuestions.get(0).id() : null;
            items.add(new OverviewResponse.AttentionItem(
                    "QUESTION_IA",
                    unanswered + (unanswered > 1 ? " questions sans réponse" : " question sans réponse"),
                    "SOON",
                    new OverviewResponse.Action("ANSWER_QUESTION", "analyse", "ai-questions", null),
                    questionId, null));
        }

        // Analyse obsolète (pièces en attente).
        if (analysisStale) {
            items.add(new OverviewResponse.AttentionItem(
                    "ANALYSE_OBSOLETE",
                    pendingPiecesCount + (pendingPiecesCount > 1
                            ? " pièces non analysées" : " pièce non analysée"),
                    "INFO",
                    new OverviewResponse.Action(
                            "RELAUNCH_ANALYSIS", "analyse", null,
                            "/api/v1/case-files/{caseFileId}/re-analyze"),
                    null, null));
        }

        items.sort(Comparator.comparingInt(i -> URGENCY_RANK.getOrDefault(i.urgency(), 99)));

        // F-289 (fix) — sélection ÉQUILIBRÉE avant la troncature à ATTENTION_LIMIT.
        // Sans ça, une rafale d'échéances OVERDUE/CRITICAL (rang le plus haut)
        // remplit les 5 slots et ÉJECTE la question IA / les pièces (rang SOON)
        // — bug constaté au test. On garantit qu'au moins le 1ᵉʳ item de chaque
        // TYPE présent figure en tête (≤ 4 types → tient dans le cap), en
        // préservant l'ordre d'urgence entre représentants, puis on complète le
        // reste par urgence. L'appelant tronque ensuite à 5.
        List<OverviewResponse.AttentionItem> primary = new ArrayList<>();
        List<OverviewResponse.AttentionItem> rest = new ArrayList<>();
        Set<String> seenTypes = new HashSet<>();
        for (OverviewResponse.AttentionItem it : items) {
            if (seenTypes.add(it.type())) {
                primary.add(it);
            } else {
                rest.add(it);
            }
        }
        primary.addAll(rest);
        return primary;
    }

    // ── ③ Fil chronologique ────────────────────────────────────────────────────

    private List<OverviewResponse.TimelineEvent> buildFil(UUID caseFileId,
                                                          CasePhaseTimelineResponse phases,
                                                          ContradictoireTimelineResponse contradictoire,
                                                          PiecesWaveResponse wave,
                                                          CaseFileDashboardResponse dashboard,
                                                          EcheancierResponse echeancier,
                                                          LocalDate today) {
        List<OverviewResponse.TimelineEvent> fil = new ArrayList<>();

        // Phases → PROCEDURE (avec pièces versées dans la fenêtre de chaque phase).
        if (phases != null && phases.phases() != null) {
            // Phases datées, triées par enteredAt → permet de calculer les fenêtres
            // [enteredAt(phase), enteredAt(phase suivante)[ (dernière phase : ouverte).
            List<CasePhaseResponse> dated = phases.phases().stream()
                    .filter(p -> p.enteredAt() != null)
                    .sorted(Comparator.comparing(CasePhaseResponse::enteredAt))
                    .toList();
            // Documents du dossier, lus une fois (fail-open : si jette, fenêtres vides).
            List<Document> caseDocuments = safe(
                    () -> documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId),
                    List.of());
            for (int i = 0; i < dated.size(); i++) {
                CasePhaseResponse p = dated.get(i);
                LocalDate windowStart = p.enteredAt();
                // Borne haute exclusive = entrée de la phase suivante ; dernière phase → +∞.
                LocalDate windowEnd = i + 1 < dated.size() ? dated.get(i + 1).enteredAt() : null;
                List<OverviewResponse.Attachment> attachments =
                        documentsInWindow(caseDocuments, windowStart, windowEnd);
                fil.add(new OverviewResponse.TimelineEvent(
                        p.enteredAt(), "PROCEDURE", "SYSTEME",
                        p.label() != null ? p.label() : p.phase().name(),
                        p.note(), temps(p.enteredAt(), today), null,
                        attachments,
                        new OverviewResponse.Action("NAVIGATE", "suivi", "phases", null)));
            }
        }

        // Rounds contradictoires → ECHANGE.
        if (contradictoire != null && contradictoire.rounds() != null) {
            for (ContradictoireRoundResponse r : contradictoire.rounds()) {
                if (r.datedAt() == null) {
                    continue;
                }
                List<OverviewResponse.Attachment> attachments = roundAttachments(r);
                String acteur = r.party() != null ? r.party().name() : null;
                // Round « à vous » (adverse a déposé) → CTA génération de réplique.
                OverviewResponse.Action action = "ADVERSE".equals(acteur)
                        ? new OverviewResponse.Action("GENERATE_REPLY", "decision", null, null)
                        : new OverviewResponse.Action("NAVIGATE", "suivi", "contradictoire", null);
                fil.add(new OverviewResponse.TimelineEvent(
                        r.datedAt(), "ECHANGE", acteur,
                        r.label() != null ? r.label() : "Round " + r.roundNumber(),
                        null, temps(r.datedAt(), today), null, attachments, action));
            }
        }

        // Vague de pièces → PIECES (1 event groupé).
        if (wave != null && wave.pendingCount() > 0 && wave.pendingPieces() != null) {
            LocalDate date = wave.pendingPieces().stream()
                    .map(pp -> toLocalDate(pp.createdAt()))
                    .filter(java.util.Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(today);
            List<OverviewResponse.Attachment> attachments = wave.pendingPieces().stream()
                    .map(pp -> new OverviewResponse.Attachment(pp.documentId(), pp.filename()))
                    .toList();
            String titre = "+" + wave.pendingCount()
                    + (wave.pendingCount() > 1 ? " pièces ajoutées" : " pièce ajoutée");
            fil.add(new OverviewResponse.TimelineEvent(
                    date, "PIECES", "OURS", titre, "En attente d'analyse",
                    temps(date, today), null, attachments,
                    new OverviewResponse.Action("RELAUNCH_ANALYSIS", "analyse", null,
                            "/api/v1/case-files/{caseFileId}/re-analyze")));
        }

        // Production : analyse DONE, stratégie, versions de conclusions.
        fil.addAll(buildProductionEvents(caseFileId, dashboard, today));

        // Échéances futures → ECHEANCE.
        if (echeancier != null && echeancier.items() != null) {
            for (EcheancierItem e : echeancier.items()) {
                if (e.dueDate() == null || !e.dueDate().isAfter(today)) {
                    continue;
                }
                fil.add(new OverviewResponse.TimelineEvent(
                        e.dueDate(), "ECHEANCE", null, e.label(), null,
                        "FUTUR", e.urgency() != null ? e.urgency().name() : null,
                        List.of(),
                        new OverviewResponse.Action("VALIDATE_DEADLINE", "suivi", "echeancier", null)));
            }
        }

        fil.sort(Comparator.comparing(OverviewResponse.TimelineEvent::date));
        return fil;
    }

    private List<OverviewResponse.TimelineEvent> buildProductionEvents(UUID caseFileId,
                                                                       CaseFileDashboardResponse dashboard,
                                                                       LocalDate today) {
        List<OverviewResponse.TimelineEvent> events = new ArrayList<>();

        // Dernière analyse dossier DONE (fail-open).
        safeRun(() -> {
            analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS)
                    .filter(j -> j.getStatus() == AnalysisStatus.DONE)
                    .map(AnalysisJob::getUpdatedAt)
                    .map(this::toLocalDate)
                    .ifPresent(date -> events.add(new OverviewResponse.TimelineEvent(
                            date, "PRODUCTION", "SYSTEME", "Analyse effectuée", null,
                            temps(date, today), null, List.of(),
                            new OverviewResponse.Action("NAVIGATE", "analyse", null, null))));
        });

        // Stratégie générée (fail-open).
        safeRun(() -> strategyRepository.findByCaseFileId(caseFileId)
                .map(CaseStrategy::getGeneratedAt)
                .map(this::toLocalDate)
                .ifPresent(date -> events.add(new OverviewResponse.TimelineEvent(
                        date, "PRODUCTION", "SYSTEME", "Stratégie générée", null,
                        temps(date, today), null, List.of(),
                        new OverviewResponse.Action("NAVIGATE", "decision", "strategie", null)))));

        // Versions de conclusions générées (fail-open).
        safeRun(() -> {
            for (CaseConclusion c : conclusionRepository.findByCaseFileIdOrderByVersionNumberDesc(caseFileId)) {
                LocalDate date = toLocalDate(c.getGeneratedAt());
                if (date == null) {
                    continue;
                }
                events.add(new OverviewResponse.TimelineEvent(
                        date, "PRODUCTION", "SYSTEME",
                        "Conclusions v" + c.getVersionNumber() + " générées", null,
                        temps(date, today), null, List.of(),
                        new OverviewResponse.Action("NAVIGATE", "decision", "conclusions", null)));
            }
        });

        return events;
    }

    /**
     * SF-289-03 — pièces dont la date de création tombe dans la fenêtre d'une phase
     * {@code [windowStart, windowEnd[} ({@code windowEnd == null} → fenêtre ouverte
     * jusqu'à maintenant, pour la dernière phase). La comparaison se fait au jour (la
     * date de la phase est un {@link LocalDate}). Triées de la plus récente à la plus
     * ancienne (l'entrée repository l'est déjà). Fail-open : un document à date nulle
     * est ignoré ; deux phases de même date → fenêtre vide acceptable.
     */
    private List<OverviewResponse.Attachment> documentsInWindow(List<Document> caseDocuments,
                                                                LocalDate windowStart,
                                                                LocalDate windowEnd) {
        if (caseDocuments == null || caseDocuments.isEmpty()) {
            return List.of();
        }
        List<OverviewResponse.Attachment> attachments = new ArrayList<>();
        for (Document d : caseDocuments) {
            LocalDate created = toLocalDate(d.getCreatedAt());
            if (created == null || created.isBefore(windowStart)) {
                continue;
            }
            // Borne haute exclusive : created < windowEnd (sauf dernière phase, ouverte).
            if (windowEnd != null && !created.isBefore(windowEnd)) {
                continue;
            }
            attachments.add(new OverviewResponse.Attachment(d.getId(), d.getOriginalFilename()));
        }
        return attachments;
    }

    private List<OverviewResponse.Attachment> roundAttachments(ContradictoireRoundResponse r) {
        List<OverviewResponse.Attachment> attachments = new ArrayList<>();
        if (r.sourceDocumentId() != null) {
            // Document supprimé entre-temps → ligne masquée (pas d'erreur).
            documentRepository.findById(r.sourceDocumentId())
                    .ifPresent(d -> attachments.add(
                            new OverviewResponse.Attachment(d.getId(), d.getOriginalFilename())));
        }
        if (r.sourceConclusionId() != null) {
            attachments.add(new OverviewResponse.Attachment(r.sourceConclusionId(), "Conclusions"));
        }
        return attachments;
    }

    // ── Utilitaires ─────────────────────────────────────────────────────────

    /** {@code temps} dérivé : date<aujourd'hui→PASSE ; ==→AUJOURDHUI ; >→FUTUR. */
    static String temps(LocalDate date, LocalDate today) {
        if (date.isBefore(today)) {
            return "PASSE";
        }
        if (date.isAfter(today)) {
            return "FUTUR";
        }
        return "AUJOURDHUI";
    }

    private LocalDate toLocalDate(java.time.Instant instant) {
        return instant == null ? null : LocalDate.ofInstant(instant, ZoneId.systemDefault());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String safeLabel(String s) {
        return s == null ? "" : s;
    }

    /** Exécute un fournisseur fail-open : exception → valeur de repli + log. */
    private <T> T safe(java.util.function.Supplier<T> supplier, T fallback) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("F-289 — source de la vue d'ensemble indisponible : {}", e.getMessage());
            return fallback;
        }
    }

    /** Exécute un bloc fail-open (effet de bord sur une liste accumulée). */
    private void safeRun(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("F-289 — contributeur du fil indisponible : {}", e.getMessage());
        }
    }

    // ── Isolation workspace (mirror ContradictoireService) ─────────────────────

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private CaseFile resolveCaseFileForUser(UUID caseFileId, User user) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(caseFile.getWorkspace().getId()))
                .orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        return caseFile;
    }
}
