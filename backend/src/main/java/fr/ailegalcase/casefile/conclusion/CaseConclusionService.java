package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.analysis.JurisprudenceCheck;
import fr.ailegalcase.analysis.JurisprudenceCheckRepository;
import fr.ailegalcase.analysis.JurisprudenceCheckStatus;
import fr.ailegalcase.analysis.StrategicOption;
import fr.ailegalcase.analysis.StrategicOptionRepository;
import fr.ailegalcase.analysis.StrategicOptionStatus;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileDashboardService;
import fr.ailegalcase.casefile.DashboardTile;
import fr.ailegalcase.casefile.ProcedureStageCatalog;
import fr.ailegalcase.casefile.jurisprudence.JurisprudenceCitation;
import fr.ailegalcase.casefile.jurisprudence.JurisprudenceCitationRepository;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
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
    private final DocumentPieceRepository documentPieceRepository;
    private final CaseFileDashboardService caseFileDashboardService;
    private final CaseConclusionPromptBuilder promptBuilder;
    private final AnthropicService anthropicService;
    private final StyleCorpusRepository styleCorpusRepository;
    private final JurisprudenceCitationRepository jurisprudenceCitationRepository;
    private final JurisprudenceCheckRepository jurisprudenceCheckRepository;
    private final fr.ailegalcase.jurisprudencemapping.ConclusionsJurisprudenceContext toolJurisprudenceContext;
    private final DocumentRepository documentRepository;
    private final DocumentExtractionRepository documentExtractionRepository;
    private final AdverseMoyensExtractor adverseMoyensExtractor;

    /** Auto-référence pour franchir le proxy transactionnel depuis le listener. */
    @Lazy
    @Autowired
    private CaseConclusionService self;

    public CaseConclusionService(CaseConclusionRepository caseConclusionRepository,
                                 CaseAnalysisRepository caseAnalysisRepository,
                                 StrategicOptionRepository strategicOptionRepository,
                                 DocumentPieceRepository documentPieceRepository,
                                 CaseFileDashboardService caseFileDashboardService,
                                 CaseConclusionPromptBuilder promptBuilder,
                                 AnthropicService anthropicService,
                                 StyleCorpusRepository styleCorpusRepository,
                                 JurisprudenceCitationRepository jurisprudenceCitationRepository,
                                 JurisprudenceCheckRepository jurisprudenceCheckRepository,
                                 fr.ailegalcase.jurisprudencemapping.ConclusionsJurisprudenceContext toolJurisprudenceContext,
                                 DocumentRepository documentRepository,
                                 DocumentExtractionRepository documentExtractionRepository,
                                 AdverseMoyensExtractor adverseMoyensExtractor) {
        this.caseConclusionRepository = caseConclusionRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.strategicOptionRepository = strategicOptionRepository;
        this.documentPieceRepository = documentPieceRepository;
        this.caseFileDashboardService = caseFileDashboardService;
        this.promptBuilder = promptBuilder;
        this.anthropicService = anthropicService;
        this.styleCorpusRepository = styleCorpusRepository;
        this.jurisprudenceCitationRepository = jurisprudenceCitationRepository;
        this.jurisprudenceCheckRepository = jurisprudenceCheckRepository;
        this.toolJurisprudenceContext = toolJurisprudenceContext;
        this.documentRepository = documentRepository;
        this.documentExtractionRepository = documentExtractionRepository;
        this.adverseMoyensExtractor = adverseMoyensExtractor;
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
            AiCallContext ctx = AiCallContext.systemLevel(
                    JobType.SYSTEM_CASE_CONCLUSION, prepared.caseFileId());
            result = anthropicService.analyzeWithSystemCache(
                    ctx, prepared.systemPrompt(), prepared.userMessage(), MAX_TOKENS);
            if (result.content() == null || result.content().isBlank()) {
                throw new IllegalStateException("Réponse IA vide");
            }
        } catch (Exception e) {
            log.error("Conclusion generation FAILED — conclusion={}", caseConclusionId, e);
            failure = e;
        }

        self.finalize(caseConclusionId, result, failure, prepared.styleApplied(),
                prepared.pieces());
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

        // SF-98-57 : la liste des pièces numérotées sert à la fois au prompt (renvois
        // « Pièce n° X ») et à l'assemblage déterministe du bordereau en finalisation.
        // Chargée UNE seule fois pour garantir l'identité de source (cohérence garantie).
        List<CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece> numberedPieces =
                loadNumberedPieces(caseFileId);

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
                        numberedPieces,
                        loadDecisionToolTiles(caseFileId),
                        loadRetainedStrategies(caseFileId),
                        loadJurisprudenceCitations(caseFileId),
                        toolJurisprudenceContext.collectForCaseFile(caseFileId),
                        loadAdverseJurisprudenceChecks(caseFileId),
                        loadAdverseMoyens(caseFileId, domain, country),
                        // F-271 / SF-271-02 — base récapitulative (texte de l'avocat à
                        // préserver) : content de la dernière version DONE, bordereau retiré.
                        // En mode « Régénérer de zéro » (generatedFromScratch), aucune base.
                        conclusion.isGeneratedFromScratch()
                                ? null
                                : loadPreviousRecapContent(caseFileId, conclusion.getId()));

        List<String> styleSignatures = loadActiveStyleSignatures(conclusion.getWorkspace().getId());
        CombinationKey key = new CombinationKey(domain, country, conclusion.getJurisdictionCode(),
                conclusion.getStageCode(), conclusion.getPositionCode());
        String systemPrompt = promptBuilder.buildSystemPrompt(key, styleSignatures);
        String userMessage = promptBuilder.buildUserMessage(input);
        return new PreparedConclusion(systemPrompt, userMessage, !styleSignatures.isEmpty(),
                caseFileId, numberedPieces);
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
     * @param pieces       SF-98-57 — pièces numérotées du dossier (même liste que celle
     *                     injectée au prompt) pour assembler le bordereau en annexe
     */
    @Transactional
    public void finalize(UUID caseConclusionId, AnthropicResult result, Exception failure,
                         boolean styleApplied,
                         List<CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece> pieces) {
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
        // SF-98-57 : annexe « Bordereau de pièces communiquées » assemblée de façon
        // déterministe (hors-LLM) à partir des pièces déjà injectées au prompt → zéro
        // pièce hallucinée et numéros cohérents avec les renvois « Pièce n° X » du corps.
        // Liste vide/null → chaîne vide → aucune section ajoutée.
        conclusion.setContent(result.content() + BordereauPiecesBuilder.buildBordereau(pieces));
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

    /**
     * F-271 — base récapitulative : {@code content} de la dernière version {@code DONE}
     * du dossier (éditions manuelles de l'avocat incluses), à consolider en jeu
     * récapitulatif (art. 768 CPC).
     *
     * <p>Retourne {@code null} (génération from-scratch) à la première génération, si la
     * dernière version DONE a un content vide, ou si la seule version DONE est
     * {@code currentId} lui-même (jamais sa propre base). <strong>Fail-open</strong> :
     * toute exception est avalée (log warn) et la génération se poursuit sans base.</p>
     *
     * <p>SF-271-02 — le <strong>bordereau de pièces</strong> est <em>retiré</em> de la
     * base avant réinjection : {@code finalize} l'ajoute de façon déterministe (hors-LLM)
     * à chaque version, donc le réinjecter inviterait le modèle à le recopier puis un
     * second bordereau serait concaténé → doublon à chaque régénération. On coupe au
     * premier marqueur {@link BordereauPiecesBuilder#SECTION_TITLE}.</p>
     *
     * @param caseFileId dossier
     * @param currentId  id de la version en cours de génération (à exclure)
     * @return le content précédent à consolider (bordereau retiré), ou {@code null}
     */
    private String loadPreviousRecapContent(UUID caseFileId, UUID currentId) {
        try {
            return caseConclusionRepository
                    .findFirstByCaseFileIdAndStatusOrderByVersionNumberDesc(
                            caseFileId, CaseConclusionStatus.DONE)
                    .filter(c -> !c.getId().equals(currentId))
                    .map(CaseConclusion::getContent)
                    .map(CaseConclusionService::stripBordereau)
                    .filter(content -> content != null && !content.isBlank())
                    .orElse(null);
        } catch (RuntimeException ex) {
            log.warn("Lecture de la base récapitulative échouée — caseFile={}, current={} — "
                    + "fail-open: génération sans base", caseFileId, currentId, ex);
            return null;
        }
    }

    /**
     * SF-271-02 — retire la section bordereau du content : coupe au premier marqueur
     * {@link BordereauPiecesBuilder#SECTION_TITLE} (le bordereau étant toujours en fin
     * d'acte, assemblé par {@code finalize}). {@code null} en entrée → {@code null}.
     * Sans marqueur, le content est rendu inchangé (puis re-trimé en aval).
     */
    private static String stripBordereau(String content) {
        if (content == null) {
            return null;
        }
        int marker = content.indexOf(BordereauPiecesBuilder.SECTION_TITLE);
        if (marker < 0) {
            return content;
        }
        return content.substring(0, marker).stripTrailing();
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
        // F-260 / SF-260-01 : lit le numéro de pièce PERSISTANT (piece_number) au lieu
        // de le recalculer à la volée (createdAt DESC → number++). Source unique
        // partagée avec les fiches (prud'homale / tribunal du travail). Fallback sur
        // l'ordre persistant (orderIndex) pour les pièces antérieures au backfill.
        List<CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece> pieces = new ArrayList<>();
        int fallback = 1;
        for (DocumentPiece piece : documentPieceRepository.findByCaseFileIdOrderByPieceNumber(caseFileId)) {
            int number = piece.getPieceNumber() != null ? piece.getPieceNumber() : fallback;
            pieces.add(new CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece(
                    number,
                    piece.getLabel(),
                    piece.getType() != null ? piece.getType().name() : null));
            fallback++;
        }
        // SF-260-02 : filet de lecture. Si des numéros en collision subsistent dans la
        // liste chargée (bordereau « 1, 1, 1, … » hérité d'une race d'écriture passée
        // non encore réparée), renuméroter POSITIONNELLEMENT 1..N. La requête est déjà
        // triée (piece_number NULLS LAST, createdAt, orderIndex) → ordre déterministe.
        // No-op sur données saines : numéros déjà distincts 1..N ⇒ aucune modification.
        if (hasDuplicateNumbers(pieces)) {
            List<CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece> renumbered =
                    new ArrayList<>(pieces.size());
            int n = 1;
            for (CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece p : pieces) {
                renumbered.add(new CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece(
                        n++, p.label(), p.type()));
            }
            return renumbered;
        }
        return pieces;
    }

    /** SF-260-02 : vrai si deux pièces partagent le même numéro (collision résiduelle). */
    private static boolean hasDuplicateNumbers(
            List<CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece> pieces) {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece p : pieces) {
            if (!seen.add(p.number())) return true;
        }
        return false;
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

    /**
     * F-242 / SF-242-01 — citations de jurisprudence d'appui du dossier, triées par
     * point juridique (ordre stable garanti par le repository).
     *
     * <p><strong>Fail-open</strong> : si la lecture des citations échoue, la génération
     * se poursuit sans la section jurisprudence — l'exception n'est jamais propagée,
     * seulement journalisée.</p>
     */
    private List<CaseConclusionPromptBuilder.ConclusionPromptInput.JurisprudenceCitationForPrompt>
            loadJurisprudenceCitations(UUID caseFileId) {
        try {
            List<CaseConclusionPromptBuilder.ConclusionPromptInput.JurisprudenceCitationForPrompt>
                    citations = new ArrayList<>();
            for (JurisprudenceCitation citation : jurisprudenceCitationRepository
                    .findByCaseFileIdOrderByPointJuridiqueIndexAscCreatedAtAsc(caseFileId)) {
                citations.add(
                        new CaseConclusionPromptBuilder.ConclusionPromptInput.JurisprudenceCitationForPrompt(
                                citation.getPointJuridiqueIndex(),
                                citation.getPointJuridiqueTexte(),
                                citation.getReference(),
                                citation.getPortee()));
            }
            return citations;
        } catch (RuntimeException ex) {
            log.warn("Lecture des citations de jurisprudence indisponible pour caseFile {} — "
                    + "génération sans section jurisprudence : {}", caseFileId, ex.getMessage());
            return List.of();
        }
    }

    /**
     * F-98 / SF-98-56 — citations de jurisprudence adverse à réfuter : checks du dossier
     * marqués par l'avocat ET de statut réfutable ({@code SUSPECT} / {@code NOT_FOUND}).
     * Mappés vers l'intrant {@link CaseConclusionPromptBuilder.ConclusionPromptInput.AdverseCitationToRefute}
     * qui n'expose ni nom de fichier ni statut technique (anti-jargon SF-98-55).
     *
     * <p><strong>Fail-open</strong> : si la lecture échoue, la génération se poursuit sans
     * section de réfutation — l'exception n'est jamais propagée, seulement journalisée.</p>
     */
    private List<CaseConclusionPromptBuilder.ConclusionPromptInput.AdverseCitationToRefute>
            loadAdverseJurisprudenceChecks(UUID caseFileId) {
        try {
            List<CaseConclusionPromptBuilder.ConclusionPromptInput.AdverseCitationToRefute>
                    adverse = new ArrayList<>();
            for (JurisprudenceCheck check : jurisprudenceCheckRepository
                    .findByCaseFileIdAndStatutInAndMarkedAdverseTrue(
                            caseFileId,
                            List.of(JurisprudenceCheckStatus.SUSPECT, JurisprudenceCheckStatus.NOT_FOUND))) {
                adverse.add(
                        new CaseConclusionPromptBuilder.ConclusionPromptInput.AdverseCitationToRefute(
                                check.getReference(),
                                check.getExplication(),
                                check.getPositionAlleguee()));
            }
            return adverse;
        } catch (RuntimeException ex) {
            log.warn("Lecture des citations adverses à réfuter indisponible pour caseFile {} — "
                    + "génération sans section de réfutation : {}", caseFileId, ex.getMessage());
            return List.of();
        }
    }

    /**
     * F-261 / SF-261-02 — moyens de la partie adverse extraits des documents marqués
     * « écritures adverses » ({@code adverse_pleadings}, SF-261-01) du dossier.
     *
     * <p>Charge le texte extrait de chaque document adverse ({@link DocumentExtraction})
     * puis délègue l'extraction à {@link AdverseMoyensExtractor} (appel LLM uniquement
     * pour le travail FR avec texte exploitable ; no-op sinon). <strong>Aucun document
     * adverse → aucun appel LLM</strong> (liste de textes vide).</p>
     *
     * <p><strong>Fail-open</strong> : toute erreur de lecture aboutit à une liste vide
     * (génération sans section), l'exception n'est jamais propagée. L'extracteur est
     * lui-même fail-open sur l'appel IA.</p>
     */
    private List<AdverseMoyen> loadAdverseMoyens(UUID caseFileId, String domain, String country) {
        try {
            List<String> textesAdverses = new ArrayList<>();
            for (Document document : documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId)) {
                if (!document.isAdversePleadings()) {
                    continue;
                }
                documentExtractionRepository.findByDocumentId(document.getId())
                        .map(DocumentExtraction::getExtractedText)
                        .filter(text -> text != null && !text.isBlank())
                        .ifPresent(textesAdverses::add);
            }
            if (textesAdverses.isEmpty()) {
                return List.of();
            }
            return adverseMoyensExtractor.extract(textesAdverses, domain, country, caseFileId);
        } catch (RuntimeException ex) {
            log.warn("Lecture des écritures adverses indisponible pour caseFile {} — "
                    + "génération sans section de réfutation des moyens : {}", caseFileId, ex.getMessage());
            return List.of();
        }
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
     * @param caseFileId   F-257 — identifiant du dossier rattaché, propagé dans le
     *                     {@link AiCallContext} pour traçabilité {@code usage_events}
     * @param pieces       SF-98-57 — pièces numérotées chargées une seule fois, partagées
     *                     entre le prompt et l'assemblage du bordereau (source unique)
     */
    record PreparedConclusion(String systemPrompt, String userMessage, boolean styleApplied,
                              UUID caseFileId,
                              List<CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece> pieces) {
    }
}
