package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.StrategicOption;
import fr.ailegalcase.analysis.StrategicOptionRepository;
import fr.ailegalcase.analysis.StrategicOptionStatus;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileDashboardService;
import fr.ailegalcase.casefile.DashboardTile;
import fr.ailegalcase.casefile.ProcedureStageCatalog;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentPiece;
import fr.ailegalcase.document.DocumentPieceRepository;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.stylelearning.StyleCorpusDocument;
import fr.ailegalcase.stylelearning.StyleCorpusDocumentStatus;
import fr.ailegalcase.stylelearning.StyleCorpusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * F-98 / SF-98-01 — worker asynchrone de génération du projet de conclusions.
 *
 * <p>Consomme la queue {@code case.conclusion} : assemble le prompt via
 * {@link CaseConclusionPromptBuilder} (stade procédural + synthèse + pièces +
 * verdicts des outils décisionnels + pistes stratégiques retenues), appelle
 * {@link AnthropicService#analyzeWithSystemCache} (Sonnet, {@code maxTokens = 8000}),
 * puis persiste {@code DONE} (content + tokens) ou {@code FAILED} (errorMessage).</p>
 *
 * <p>Profil {@code local}/{@code prod} uniquement, aligné sur les autres workers
 * {@code @RabbitListener}. En test, le worker est instancié directement et ses
 * méthodes appelées sans RabbitMQ.</p>
 */
@Service
@Profile({"local", "prod"})
public class CaseConclusionService {

    private static final Logger log = LoggerFactory.getLogger(CaseConclusionService.class);

    /** Budget de tokens de sortie — cf. mini-spec (qualité rédactionnelle). */
    static final int MAX_TOKENS = 8000;

    private final CaseConclusionRepository caseConclusionRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final StrategicOptionRepository strategicOptionRepository;
    private final DocumentRepository documentRepository;
    private final DocumentPieceRepository documentPieceRepository;
    private final CaseFileDashboardService caseFileDashboardService;
    private final CaseConclusionPromptBuilder promptBuilder;
    private final AnthropicService anthropicService;
    private final StyleCorpusRepository styleCorpusRepository;

    /** Auto-référence pour franchir le proxy transactionnel depuis le listener. */
    @Lazy
    @Autowired
    private CaseConclusionService self;

    public CaseConclusionService(CaseConclusionRepository caseConclusionRepository,
                                 CaseAnalysisRepository caseAnalysisRepository,
                                 StrategicOptionRepository strategicOptionRepository,
                                 DocumentRepository documentRepository,
                                 DocumentPieceRepository documentPieceRepository,
                                 CaseFileDashboardService caseFileDashboardService,
                                 CaseConclusionPromptBuilder promptBuilder,
                                 AnthropicService anthropicService,
                                 StyleCorpusRepository styleCorpusRepository) {
        this.caseConclusionRepository = caseConclusionRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.strategicOptionRepository = strategicOptionRepository;
        this.documentRepository = documentRepository;
        this.documentPieceRepository = documentPieceRepository;
        this.caseFileDashboardService = caseFileDashboardService;
        this.promptBuilder = promptBuilder;
        this.anthropicService = anthropicService;
        this.styleCorpusRepository = styleCorpusRepository;
    }

    @RabbitListener(queues = CaseConclusionRabbitMQConfig.CASE_CONCLUSION_QUEUE, concurrency = "2")
    public void consumeCaseConclusion(CaseConclusionMessage message) {
        generate(message.caseConclusionId());
    }

    /**
     * Génère le projet de conclusions pour une ligne {@code case_conclusions}.
     *
     * <p>Découpé en 3 phases : préparation transactionnelle ({@code PROCESSING} +
     * assemblage du prompt), appel IA hors transaction, finalisation transactionnelle
     * ({@code DONE} ou {@code FAILED}). Une exception de l'appel IA n'interrompt pas
     * le traitement : elle aboutit à {@code FAILED} avec {@code errorMessage}.</p>
     *
     * @param caseConclusionId identifiant de la ligne à traiter
     */
    public void generate(UUID caseConclusionId) {
        PreparedConclusion prepared = self.prepare(caseConclusionId);
        if (prepared == null) {
            return;
        }

        AnthropicResult result = null;
        Exception failure = null;
        try {
            log.info("Conclusion generation START — conclusion={} ({} chars user message)",
                    caseConclusionId, prepared.userMessage().length());
            result = anthropicService.analyzeWithSystemCache(
                    prepared.systemPrompt(), prepared.userMessage(), MAX_TOKENS);
            if (result.content() == null || result.content().isBlank()) {
                throw new IllegalStateException("Réponse IA vide");
            }
        } catch (Exception e) {
            log.error("Conclusion generation FAILED — conclusion={}", caseConclusionId, e);
            failure = e;
        }

        self.finalize(caseConclusionId, result, failure, prepared.styleApplied());
    }

    /**
     * Phase 1 — passe la ligne à {@code PROCESSING} et assemble le prompt.
     *
     * @return les données prêtes pour l'appel IA, ou {@code null} si la ligne est
     *         introuvable / n'est plus {@code PENDING} (idempotence sur double message)
     */
    @Transactional
    public PreparedConclusion prepare(UUID caseConclusionId) {
        CaseConclusion conclusion = caseConclusionRepository.findById(caseConclusionId).orElse(null);
        if (conclusion == null) {
            log.warn("Conclusion {} introuvable — message ignoré", caseConclusionId);
            return null;
        }
        if (conclusion.getStatus() != CaseConclusionStatus.PENDING) {
            log.warn("Conclusion {} déjà au statut {} — message ignoré", caseConclusionId,
                    conclusion.getStatus());
            return null;
        }

        conclusion.setStatus(CaseConclusionStatus.PROCESSING);
        caseConclusionRepository.save(conclusion);

        CaseFile caseFile = conclusion.getCaseFile();
        UUID caseFileId = caseFile.getId();
        String domain = caseFile.getLegalDomain();
        String country = conclusion.getWorkspace().getCountry();

        CaseConclusionPromptBuilder.ConclusionPromptInput input =
                new CaseConclusionPromptBuilder.ConclusionPromptInput(
                        caseFile.getTitle(),
                        labelSafe(() -> ProcedureStageCatalog.jurisdictionLabel(
                                domain, country, conclusion.getJurisdictionCode())),
                        labelSafe(() -> ProcedureStageCatalog.stageLabel(
                                domain, country, conclusion.getStageCode())),
                        labelSafe(() -> ProcedureStageCatalog.positionLabel(
                                domain, country, conclusion.getPositionCode())),
                        loadLatestAnalysisResult(caseFileId),
                        loadNumberedPieces(caseFileId),
                        loadDecisionToolTiles(caseFileId),
                        loadRetainedStrategies(caseFileId));

        List<String> styleSignatures = loadActiveStyleSignatures(conclusion.getWorkspace().getId());
        CombinationKey key = new CombinationKey(domain, country, conclusion.getJurisdictionCode(),
                conclusion.getStageCode(), conclusion.getPositionCode());
        String systemPrompt = promptBuilder.buildSystemPrompt(key, styleSignatures);
        String userMessage = promptBuilder.buildUserMessage(input);
        return new PreparedConclusion(systemPrompt, userMessage, !styleSignatures.isEmpty());
    }

    /**
     * F-98 / SF-98-47 — signatures de style exploitables du workspace : documents du
     * corpus de style {@code active = true} ET {@code status = DONE}, dont la
     * {@code styleSignature} est renseignée.
     *
     * <p><strong>Fail-open</strong> : si la lecture du corpus de style échoue, la
     * génération se poursuit en mode générique (aucune signature) — l'exception n'est
     * jamais propagée, seulement journalisée en avertissement.</p>
     *
     * @return les descriptions de style actives (jamais {@code null} ; vide si aucune
     *         signature exploitable ou en cas d'échec de lecture)
     */
    private List<String> loadActiveStyleSignatures(UUID workspaceId) {
        try {
            List<String> signatures = new ArrayList<>();
            for (StyleCorpusDocument document : styleCorpusRepository
                    .findByWorkspaceIdAndActiveTrueAndStatus(workspaceId, StyleCorpusDocumentStatus.DONE)) {
                String signature = document.getStyleSignature();
                if (signature != null && !signature.isBlank()) {
                    signatures.add(signature);
                }
            }
            return signatures;
        } catch (RuntimeException ex) {
            log.warn("Lecture des signatures de style indisponible pour workspace {} — "
                    + "génération générique : {}", workspaceId, ex.getMessage());
            return List.of();
        }
    }

    /**
     * Phase 3 — persiste le résultat : {@code DONE} (content + tokens + modèle) si
     * l'appel IA a réussi, {@code FAILED} (errorMessage) sinon.
     *
     * @param styleApplied SF-98-47 — vrai si la génération a adopté au moins une
     *                     signature de style active du workspace
     */
    @Transactional
    public void finalize(UUID caseConclusionId, AnthropicResult result, Exception failure,
                         boolean styleApplied) {
        CaseConclusion conclusion = caseConclusionRepository.findById(caseConclusionId).orElse(null);
        if (conclusion == null) {
            log.warn("Conclusion {} introuvable à la finalisation — résultat perdu", caseConclusionId);
            return;
        }

        if (failure != null || result == null) {
            conclusion.setStatus(CaseConclusionStatus.FAILED);
            conclusion.setErrorMessage(failure != null
                    ? truncate(failure.getMessage(), 2000)
                    : "Échec de la génération (résultat indisponible).");
            caseConclusionRepository.save(conclusion);
            log.info("Conclusion generation marked FAILED — conclusion={}", caseConclusionId);
            return;
        }

        conclusion.setStatus(CaseConclusionStatus.DONE);
        conclusion.setContent(result.content());
        conclusion.setModelUsed(result.modelUsed());
        conclusion.setPromptTokens(result.promptTokens());
        conclusion.setCompletionTokens(result.completionTokens());
        conclusion.setStyleApplied(styleApplied);
        conclusion.setGeneratedAt(Instant.now());
        conclusion.setErrorMessage(null);
        caseConclusionRepository.save(conclusion);
        log.info("Conclusion generation DONE — conclusion={}, tokens {}/{}", caseConclusionId,
                result.promptTokens(), result.completionTokens());
    }

    /** JSON brut de la synthèse {@code DONE} la plus récente, ou {@code null}. */
    private String loadLatestAnalysisResult(UUID caseFileId) {
        return caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .map(CaseAnalysis::getAnalysisResult)
                .orElse(null);
    }

    /**
     * Pièces numérotées du dossier, agrégées sur tous les documents et renumérotées
     * de façon stable et continue (1..N) pour la citation « Pièce n° X ».
     */
    private List<CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece> loadNumberedPieces(
            UUID caseFileId) {
        List<CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece> pieces = new ArrayList<>();
        int number = 1;
        for (Document document : documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId)) {
            for (DocumentPiece piece : documentPieceRepository
                    .findByDocument_IdOrderByOrderIndexAsc(document.getId())) {
                pieces.add(new CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece(
                        number++,
                        piece.getLabel(),
                        piece.getType() != null ? piece.getType().name() : null));
            }
        }
        return pieces;
    }

    /**
     * Verdicts des outils décisionnels remplis du dossier.
     *
     * <p>Arbitrage SF-98-01 : plutôt que d'injecter ~90 repositories {@code *Analysis},
     * on réutilise l'agrégation générique déjà exposée par
     * {@link CaseFileDashboardService#assembleDecisionToolTiles} (fail-open par tile).
     * Le générateur consomme exactement ce que le tableau de bord décisionnel sait
     * restituer — pas de chemin d'agrégation parallèle à maintenir.</p>
     */
    private List<DashboardTile> loadDecisionToolTiles(UUID caseFileId) {
        try {
            return caseFileDashboardService.assembleDecisionToolTiles(caseFileId);
        } catch (RuntimeException ex) {
            log.warn("Agrégation des outils décisionnels indisponible pour caseFile {} : {}",
                    caseFileId, ex.getMessage());
            return List.of();
        }
    }

    /** Pistes stratégiques au statut {@code RETAINED} de la synthèse la plus récente. */
    private List<CaseConclusionPromptBuilder.ConclusionPromptInput.RetainedStrategy> loadRetainedStrategies(
            UUID caseFileId) {
        CaseAnalysis analysis = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .orElse(null);
        if (analysis == null) {
            return List.of();
        }
        List<CaseConclusionPromptBuilder.ConclusionPromptInput.RetainedStrategy> retained = new ArrayList<>();
        for (StrategicOption option : strategicOptionRepository
                .findByCaseAnalysisIdAndStatutOrderByOrdreAsc(analysis.getId(), StrategicOptionStatus.RETAINED)) {
            retained.add(new CaseConclusionPromptBuilder.ConclusionPromptInput.RetainedStrategy(
                    option.getTexte(), option.getBaseJuridique()));
        }
        return retained;
    }

    private static String labelSafe(java.util.function.Supplier<String> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "Échec de la génération.";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    /**
     * Données prêtes pour l'appel IA, produites par {@link #prepare}.
     *
     * @param styleApplied SF-98-47 — vrai si le prompt système intègre une consigne
     *                     d'adaptation au style appris du cabinet
     */
    record PreparedConclusion(String systemPrompt, String userMessage, boolean styleApplied) {
    }
}
