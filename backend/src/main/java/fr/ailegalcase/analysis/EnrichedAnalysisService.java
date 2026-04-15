package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.StatutoryDeadlineService;
import fr.ailegalcase.chat.ChatMessage;
import fr.ailegalcase.chat.ChatMessageRepository;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Profile({"local", "prod"})
public class EnrichedAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(EnrichedAnalysisService.class);

    static final String SYSTEM_PROMPT_TEMPLATE = """
            Tu es un assistant juridique expert en %s.
            Tu reçois la synthèse globale d'un dossier juridique ainsi que les réponses de l'avocat à des questions complémentaires.
            Produis une synthèse enrichie et mise à jour en intégrant ces nouvelles informations.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu (inclure tous les champs) : {"timeline": [{"date": "YYYY-MM-DD", "evenement": "..."}], "faits": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "points_juridiques": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "risques": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "questions_ouvertes": [...], "pieces_manquantes": [...], "points_procedure": [...], "score_risque": {"niveau": "FAIBLE"|"MOYEN"|"ELEVE", "valeur": <0-100>}, "checks_a_requalifier": [{"description": "...", "nouveau_statut": "NON_COMPLIANT"|"TO_CHECK", "raison": "..."}], "type_litige_detecte": "LICENCIEMENT_SANS_CAUSE_REELLE"|"LICENCIEMENT_ECONOMIQUE"|"PRISE_ACTE_RUPTURE"|"HARCELEMENT_MORAL"|"DISCRIMINATION"|"HEURES_SUPPLEMENTAIRES"|"RAPPEL_SALAIRE"|null, "date_reference_prescription": "YYYY-MM-DD"|null, "compensation_data": {"type_rupture": "LICENCIEMENT"|"LICENCIEMENT_ECONOMIQUE"|"RUPTURE_CONVENTIONNELLE"|"DEMISSION"|"PRISE_ACTE"|"RESILIATION_JUDICIAIRE"|"LICENCIEMENT_ORDINAIRE"|"LICENCIEMENT_MANIFESTEMENT_DERAISONNABLE"|"RUPTURE_AMIABLE", "anciennete_annees": <entier>|null, "anciennete_mois": <entier>|null, "salaire_reference_mensuel": <décimal>|null}|null}
            Pour les champs "faits", "points_juridiques" et "risques", chaque élément est un objet avec "texte" (le contenu), "source" (nom exact du fichier tel qu'il apparaît dans la synthèse précédente) et "extrait" (phrase exacte tirée du document). Si la source n'est pas identifiable, utilise "source": null et "extrait": null.
            Le champ "pieces_manquantes" liste les pièces habituellement attendues dans ce type de dossier qui sont absentes des documents fournis. Chaque élément est un objet {"texte": "<description de la pièce>", "critere_code": "<code ou null>"}. "critere_code" est rempli UNIQUEMENT si l'absence de cette pièce correspond à un code surveillé :
            - Critères F-DT-08 Validité licenciement : FR_CONVOCATION, FR_ENTRETIEN, FR_DELAI_NOTIFICATION, FR_MOTIVATION, FR_MOTIF_REEL, FR_PROCEDURE_DISCIPLINAIRE, FR_ORDRE_LICENCIEMENT, BE_NOTIFICATION, BE_PREAVIS, BE_MOTIVATION, BE_AUDITION, BE_NON_DISCRIMINATION, BE_PROTECTION_SPECIALE, BE_INDEMNITE_MANIFESTE.
            - Critères F-DT-10 Validité rupture conventionnelle (France, art. L1237-11 s.) : RC_CONSENTEMENT, RC_DELAI_RETRACTATION, RC_HOMOLOGATION, RC_ASSISTANCE, RC_INDEMNITE, RC_ENTRETIENS.
            - Pièces F-FA-07 Checklist divorce : FR_ACTE_MARIAGE, FR_ACTE_NAISSANCE_EPOUX, FR_ACTE_NAISSANCE_ENFANTS, FR_LIVRET_FAMILLE, FR_JUSTIF_DOMICILE, FR_CONTRAT_MARIAGE, FR_ETAT_PATRIMOINE, FR_JUSTIF_REVENUS, FR_PIECE_IDENTITE, BE_ACTE_MARIAGE, BE_ACTE_NAISSANCE_EPOUX, BE_ACTE_NAISSANCE_ENFANTS, BE_COMPOSITION_MENAGE, BE_CONTRAT_MARIAGE, BE_CONVENTION_PREALABLE, BE_JUSTIF_REVENUS, BE_PIECE_IDENTITE.
            - Pièces F-FA-05 Partage immobilier : FA05_VALEUR_VENALE, FA05_CAPITAL_RESTANT.
            Sinon null. Rétrocompat : format string legacy accepté. Si le dossier semble complet, utilise "pieces_manquantes": [].
            Le champ "points_procedure" liste les étapes procédurales légalement requises dans ce type de dossier. Chaque élément est un objet {"texte": "<description de l'étape>", "critere_code": "<code ou null>", "expected_value": "<valeur ou null>"}. "critere_code" est rempli uniquement si le point porte sur l'un des critères surveillés :
            - Critères F-DT-08 Validité licenciement (droit du travail, binaires) : FR_CONVOCATION, FR_ENTRETIEN, FR_DELAI_NOTIFICATION, FR_MOTIVATION, FR_MOTIF_REEL, FR_PROCEDURE_DISCIPLINAIRE, FR_ORDRE_LICENCIEMENT, BE_NOTIFICATION, BE_PREAVIS, BE_MOTIVATION, BE_AUDITION, BE_NON_DISCRIMINATION, BE_PROTECTION_SPECIALE, BE_INDEMNITE_MANIFESTE. "expected_value" doit rester null.
            - Critères F-DT-10 Validité rupture conventionnelle (droit du travail France, binaires) : RC_CONSENTEMENT, RC_DELAI_RETRACTATION, RC_HOMOLOGATION, RC_ASSISTANCE, RC_INDEMNITE, RC_ENTRETIENS. VERIFIED = critère respecté, NON_COMPLIANT = critère non respecté. "expected_value" reste null.
            - Étapes F-FA-07 Checklist divorce (droit de la famille, binaires) : FR_CHOIX_AVOCATS, FR_REDACTION_CONVENTION, FR_ENVOI_LRAR, FR_DELAI_REFLEXION, FR_SIGNATURE_CONVENTION, FR_DEPOT_NOTAIRE, FR_ENREGISTREMENT, BE_CHOIX_AVOCAT, BE_REDACTION_CONVENTION, BE_REQUETE_CONJOINTE, BE_COMPARUTION, BE_JUGEMENT, BE_TRANSCRIPTION. VERIFIED = étape accomplie, NON_COMPLIANT = étape non faite. "expected_value" reste null.
            - Critère F-FA-06 Calendrier garde (droit de la famille, énuméré) : FA06_MODE_GARDE. "expected_value" obligatoire parmi : ALTERNEE_FR, DVH_CLASSIQUE_FR, DVH_ELARGI_FR (France), ALTERNEE_BE, SECONDAIRE_BE, SECONDAIRE_ELARGI_BE (Belgique).
            - Critère F-FA-05 Partage immobilier (droit de la famille, numérique) : FA05_VALEUR_VENALE, FA05_CAPITAL_RESTANT. "expected_value" obligatoire = valeur en euros en chaîne (ex. "350000" ou "120000.50").
            - Critère F-IM-05 Titre de séjour (droit de l'immigration, énuméré) : IM05_MOTIF. "expected_value" obligatoire parmi : TRAVAIL, ETUDES, FAMILLE, ASILE, AUTRE.
            - Critère F-IM-06 Recours (droit de l'immigration, énuméré) : IM06_RECOURS_TYPE. "expected_value" obligatoire parmi : RECOURS_GRACIEUX_PREFET, RECOURS_CONTENTIEUX_TA, RECOURS_CNDA (France), RECOURS_CGRA, RECOURS_CCE, RECOURS_CE_BELGIQUE (Belgique).
            - Critère F-IM-07 Droit au travail (droit de l'immigration, énuméré) : IM07_TITRE_TYPE. "expected_value" obligatoire parmi les 16 codes de titre (identiques à F-IM-05).
            - Critère F-DT-09 Type de rupture (énuméré) : DT09_TYPE_RUPTURE. Pour ce critère, renseigne obligatoirement "expected_value" avec la valeur affirmée par le point, parmi : LICENCIEMENT, LICENCIEMENT_ECONOMIQUE, RUPTURE_CONVENTIONNELLE (France), LICENCIEMENT_ORDINAIRE, RUPTURE_AMIABLE (Belgique).
            Pour tout point sans lien avec ces critères, "critere_code" et "expected_value" restent null. Rétrocompat : format string legacy accepté. Si la procédure semble conforme, utilise "points_procedure": [].
            Le champ "score_risque" est obligatoire : évalue le niveau de risque global du dossier. "niveau" est l'un de "FAIBLE", "MOYEN" ou "ELEVE". "valeur" est un entier entre 0 et 100 reflétant l'intensité du risque (0 = aucun risque, 100 = risque maximum).
            Le champ "checks_a_requalifier" liste les points procéduraux marqués "vérifiés" dans le prompt que tu estimes devoir requalifier à la lumière des nouvelles informations. Pour chaque point : "description" doit correspondre exactement au libellé fourni dans le prompt, "nouveau_statut" est "NON_COMPLIANT" si le point est manifestement non respecté ou "TO_CHECK" si des doutes subsistent, "raison" explique brièvement pourquoi ce point doit être revu. Si aucun point vérifié ne doit être requalifié, utilise "checks_a_requalifier": [].
            Le champ "compensation_data" contient les données nécessaires au pré-remplissage de F-DT-09. Il est OBLIGATOIRE dès qu'une rupture du contrat est identifiée, même si ancienneté ou salaire sont inconnus. Renseigne "type_rupture" parmi — France : "LICENCIEMENT" (cause réelle et sérieuse), "LICENCIEMENT_ECONOMIQUE", "RUPTURE_CONVENTIONNELLE", "DEMISSION", "PRISE_ACTE", "RESILIATION_JUDICIAIRE" ; Belgique : "LICENCIEMENT_ORDINAIRE", "LICENCIEMENT_MANIFESTEMENT_DERAISONNABLE", "RUPTURE_AMIABLE", "DEMISSION". Priorité des indices : lettre de licenciement > convention de rupture > décision judiciaire > allégation. Ne jamais renvoyer "type_rupture": null si compensation_data est émis : choisir la valeur la plus probable. "anciennete_annees" et "anciennete_mois" sont l'ancienneté totale (entiers). "salaire_reference_mensuel" est le salaire brut mensuel moyen de référence (décimal). Utilise null uniquement pour les champs numériques non déterminables. En cas de ruptures multiples, retiens la plus récente. Si le dossier ne relève pas du droit du travail ou si aucune rupture n'est identifiée, utilise "compensation_data": null.
            Le champ "type_litige_detecte" identifie le type principal de litige du dossier. Utilise l'une des valeurs suivantes (null si non déterminable) : %s.
            Le champ "date_reference_prescription" est la date à partir de laquelle commence le délai de prescription (ex : date de rupture du contrat, date des faits). Format ISO 8601 (YYYY-MM-DD). Utilise null si non déterminable.
            Contraintes de longueur : %d entrées timeline maximum, %d faits maximum, %d points_juridiques maximum, %d risques maximum, %d questions_ouvertes maximum, %d pièces manquantes maximum, %d points procédure maximum. Sois concis.
            """;

    static String buildSystemPrompt(String legalDomain, String country, AnalysisLimitsProperties.LevelLimits limits,
                                     List<String> litigationTypeKeys) {
        String litigationTypes = litigationTypeKeys.isEmpty()
                ? "aucun type configuré"
                : litigationTypeKeys.stream().map(k -> "\"" + k + "\"").collect(java.util.stream.Collectors.joining(", "));
        return SYSTEM_PROMPT_TEMPLATE.formatted(
                LegalDomainPromptBuilder.domainLabel(legalDomain, country),
                litigationTypes,
                limits.getTimeline(), limits.getFaits(),
                limits.getPointsJuridiques(), limits.getRisques(), limits.getQuestionsOuvertes(),
                limits.getPiecesManquantes(), limits.getPointsProcedure())
                + LegalDomainPromptBuilder.domainSpecificInstruction(legalDomain);
    }

    record PreparedEnrichedAnalysis(UUID analysisId, String prompt, String systemPrompt, UUID caseFileId,
                                     AnalysisLimitsProperties.LevelLimits limits, UUID previousAnalysisId) {}

    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final AiQuestionRepository aiQuestionRepository;
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final AnthropicService anthropicService;
    private final UsageEventService usageEventService;
    private final ApplicationEventPublisher eventPublisher;
    private final AnalysisDocumentSnapshotService analysisDocumentSnapshotService;
    private final AnalysisQaSnapshotService analysisQaSnapshotService;
    private final AnalysisLimitsProperties analysisLimitsProperties;
    private final ChatMessageRepository chatMessageRepository;
    private final ProcedureCheckService procedureCheckService;
    private final StatutoryDeadlineService statutoryDeadlineService;
    private final fr.ailegalcase.referential.LegalReferentialService legalReferentialService;

    @Lazy @Autowired
    private EnrichedAnalysisService self;

    public EnrichedAnalysisService(CaseAnalysisRepository caseAnalysisRepository,
                                   CaseFileRepository caseFileRepository,
                                   AiQuestionRepository aiQuestionRepository,
                                   AiQuestionAnswerRepository aiQuestionAnswerRepository,
                                   AnalysisJobRepository analysisJobRepository,
                                   AnthropicService anthropicService,
                                   UsageEventService usageEventService,
                                   ApplicationEventPublisher eventPublisher,
                                   AnalysisDocumentSnapshotService analysisDocumentSnapshotService,
                                   AnalysisQaSnapshotService analysisQaSnapshotService,
                                   AnalysisLimitsProperties analysisLimitsProperties,
                                   ChatMessageRepository chatMessageRepository,
                                   ProcedureCheckService procedureCheckService,
                                   StatutoryDeadlineService statutoryDeadlineService,
                                   fr.ailegalcase.referential.LegalReferentialService legalReferentialService) {
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.aiQuestionRepository = aiQuestionRepository;
        this.aiQuestionAnswerRepository = aiQuestionAnswerRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.anthropicService = anthropicService;
        this.usageEventService = usageEventService;
        this.eventPublisher = eventPublisher;
        this.analysisDocumentSnapshotService = analysisDocumentSnapshotService;
        this.analysisQaSnapshotService = analysisQaSnapshotService;
        this.analysisLimitsProperties = analysisLimitsProperties;
        this.chatMessageRepository = chatMessageRepository;
        this.procedureCheckService = procedureCheckService;
        this.statutoryDeadlineService = statutoryDeadlineService;
        this.legalReferentialService = legalReferentialService;
    }

    @RabbitListener(queues = RabbitMQConfig.RE_ANALYSIS_QUEUE, concurrency = "3")
    public void consumeReAnalysis(ReAnalysisMessage message) {
        long startMs = System.currentTimeMillis();
        UUID caseFileId = message.caseFileId();

        PreparedEnrichedAnalysis prepared = self.prepareEnrichedAnalysis(message);
        if (prepared == null) return;

        AnthropicResult result = null;
        Exception failure = null;
        try {
            log.info("Enriched analysis START for caseFile {} ({} chars)", caseFileId, prepared.prompt().length());
            long anthropicStart = System.currentTimeMillis();
            result = anthropicService.analyze(prepared.systemPrompt(), prepared.prompt(), 8192);
            long anthropicMs = System.currentTimeMillis() - anthropicStart;
            log.info("Enriched analysis DONE for caseFile {} — Anthropic {}ms, total {}ms, tokens {}/{}",
                    caseFileId, anthropicMs, System.currentTimeMillis() - startMs,
                    result.promptTokens(), result.completionTokens());
        } catch (Exception e) {
            log.error("Enriched analysis FAILED for caseFile {} (total {}ms)", caseFileId,
                    System.currentTimeMillis() - startMs, e);
            failure = e;
        }

        self.finalizeEnrichedAnalysis(prepared.analysisId(), prepared.caseFileId(), result, failure, prepared.limits(),
                prepared.previousAnalysisId());
    }

    @Transactional
    public PreparedEnrichedAnalysis prepareEnrichedAnalysis(ReAnalysisMessage message) {
        UUID caseFileId = message.caseFileId();

        AnalysisJob job = analysisJobRepository
                .findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.ENRICHED_ANALYSIS);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    j.setStatus(AnalysisStatus.PROCESSING);
                    return j;
                });

        CaseAnalysis previousAnalysis = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .orElse(null);

        if (previousAnalysis == null) {
            log.warn("No DONE case analysis found for caseFile {} — enriched analysis skipped", caseFileId);
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("No previous case analysis found");
            analysisJobRepository.save(job);
            return null;
        }

        CaseFile caseFile = caseFileRepository.findById(caseFileId).orElse(null);
        if (caseFile == null) {
            log.error("CaseFile {} not found — enriched analysis skipped", caseFileId);
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("Case file not found");
            analysisJobRepository.save(job);
            return null;
        }

        int nextVersion = caseAnalysisRepository.findMaxVersionByCaseFileId(caseFileId) + 1;

        CaseAnalysis enrichedAnalysis = new CaseAnalysis();
        enrichedAnalysis.setCaseFile(caseFile);
        enrichedAnalysis.setVersion(nextVersion);
        enrichedAnalysis.setAnalysisType(AnalysisType.ENRICHED);
        enrichedAnalysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        enrichedAnalysis = caseAnalysisRepository.save(enrichedAnalysis);

        analysisDocumentSnapshotService.snapshot(enrichedAnalysis.getId(), caseFile);
        analysisQaSnapshotService.snapshot(enrichedAnalysis.getId(), caseFileId);

        fr.ailegalcase.workspace.Workspace ws = caseFile.getWorkspace();
        String legalDomain = ws != null ? ws.getLegalDomain() : "DROIT_DU_TRAVAIL";
        String country     = ws != null ? ws.getCountry()     : "FRANCE";
        AnalysisLimitsProperties.LevelLimits limits = analysisLimitsProperties.forDomain(legalDomain).getDossier();
        List<String> litigationTypeKeys = "DROIT_DU_TRAVAIL".equals(legalDomain)
                ? legalReferentialService.getLitigationTypeKeys(country)
                : List.of();
        String systemPrompt = buildSystemPrompt(legalDomain, country, limits, litigationTypeKeys);
        String chatSummary = buildChatSummary(caseFileId);
        List<String> nonCompliantChecks;
        try {
            nonCompliantChecks = procedureCheckService.listNonCompliant(caseFile);
        } catch (Exception e) {
            log.warn("listNonCompliant failed for caseFile {} — enriched analysis will proceed without it", caseFileId, e);
            nonCompliantChecks = List.of();
        }
        List<String> toCheckChecks;
        try {
            toCheckChecks = procedureCheckService.listToCheck(caseFile);
        } catch (Exception e) {
            log.warn("listToCheck failed for caseFile {} — enriched analysis will proceed without it", caseFileId, e);
            toCheckChecks = List.of();
        }
        List<String> verifiedChecks;
        try {
            verifiedChecks = procedureCheckService.listVerified(caseFile);
        } catch (Exception e) {
            log.warn("listVerified failed for caseFile {} — enriched analysis will proceed without it", caseFileId, e);
            verifiedChecks = List.of();
        }
        String prompt = buildEnrichedPrompt(caseFileId, previousAnalysis.getAnalysisResult(), chatSummary,
                nonCompliantChecks, toCheckChecks, verifiedChecks);
        return new PreparedEnrichedAnalysis(enrichedAnalysis.getId(), prompt, systemPrompt, caseFileId, limits,
                previousAnalysis.getId());
    }

    @Transactional
    public void finalizeEnrichedAnalysis(UUID analysisId, UUID caseFileId, AnthropicResult result, Exception failure,
                                          AnalysisLimitsProperties.LevelLimits limits, UUID previousAnalysisId) {
        CaseAnalysis enrichedAnalysis = caseAnalysisRepository.findById(analysisId).orElseThrow();

        if (failure != null) {
            enrichedAnalysis.setAnalysisStatus(AnalysisStatus.FAILED);
        } else {
            String truncated = AnalysisJsonTruncator.truncateCaseAnalysis(result.content(), limits);
            enrichedAnalysis.setAnalysisResult(truncated);
            enrichedAnalysis.setModelUsed(result.modelUsed());
            enrichedAnalysis.setPromptTokens(result.promptTokens());
            enrichedAnalysis.setCompletionTokens(result.completionTokens());
            enrichedAnalysis.setAnalysisStatus(AnalysisStatus.DONE);
            CaseAnalysisResponse.populateCounts(enrichedAnalysis, truncated);
            CaseAnalysisResponse.populateRiskScore(enrichedAnalysis, truncated);
        }
        caseAnalysisRepository.save(enrichedAnalysis);

        if (failure == null) {
            procedureCheckService.createChecksWithVerifiedPropagation(enrichedAnalysis,
                    enrichedAnalysis.getAnalysisResult(), previousAnalysisId);
            statutoryDeadlineService.createStatutoryDeadlines(enrichedAnalysis,
                    enrichedAnalysis.getAnalysisResult());
        }

        AnalysisJob job = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.ENRICHED_ANALYSIS);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setProcessedItems(1);
        job.setStatus(enrichedAnalysis.getAnalysisStatus());
        if (enrichedAnalysis.getAnalysisStatus() == AnalysisStatus.FAILED) {
            job.setErrorMessage("Enriched analysis failed");
            reportJobFailureToSentry(caseFileId, JobType.ENRICHED_ANALYSIS, "Enriched analysis failed");
        }
        analysisJobRepository.save(job);

        AnalysisStatus finalStatus = enrichedAnalysis.getAnalysisStatus();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new AnalysisStatusEvent(caseFileId, finalStatus, JobType.ENRICHED_ANALYSIS));
            }
        });

        if (finalStatus == AnalysisStatus.DONE) {
            int promptTokens = enrichedAnalysis.getPromptTokens();
            int completionTokens = enrichedAnalysis.getCompletionTokens();
            caseFileRepository.findCreatedByUserIdById(caseFileId).ifPresent(userId ->
                usageEventService.record(caseFileId, userId, JobType.ENRICHED_ANALYSIS,
                        promptTokens, completionTokens));
        }
    }

    private void reportJobFailureToSentry(UUID caseFileId, JobType jobType, String errorMessage) {
        try {
            if (!Sentry.isEnabled()) return;
            SentryEvent event = new SentryEvent();
            event.setLevel(SentryLevel.ERROR);
            Message msg = new Message();
            msg.setMessage("IA job FAILED: %s for caseFile %s".formatted(jobType, caseFileId));
            event.setMessage(msg);
            event.setTag("caseFileId", caseFileId.toString());
            event.setTag("jobType", jobType.name());
            event.setTag("errorMessage", errorMessage);
            Sentry.captureEvent(event);
        } catch (Exception ex) {
            log.warn("Failed to report job failure to Sentry", ex);
        }
    }

    String buildChatSummary(UUID caseFileId) {
        List<ChatMessage> messages = chatMessageRepository.findByCaseFileIdOrderByCreatedAtAsc(caseFileId);
        if (messages.isEmpty()) return null;

        String chatText = messages.stream()
                .map(m -> "Q: %s\nR: %s".formatted(m.getQuestion(), m.getAnswer() != null ? m.getAnswer() : "(sans réponse)"))
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = """
                Tu es un assistant juridique. Résume en points clés analytiques les échanges suivants entre un avocat et l'IA sur un dossier juridique.
                Extrais uniquement les informations factuelles, les clarifications importantes et les observations de l'avocat.
                Ignore les reformulations et questions triviales. Sois concis (maximum 10 points).
                """;

        try {
            AnthropicResult result = anthropicService.analyzeFast(systemPrompt, chatText, 512);
            String summary = result.content();
            return (summary != null && !summary.isBlank()) ? summary.trim() : null;
        } catch (Exception e) {
            log.warn("Chat summary failed for caseFile {} — enriched analysis will proceed without it", caseFileId, e);
            return null;
        }
    }

    String buildEnrichedPrompt(UUID caseFileId, String previousAnalysisResult, String chatSummary,
                                List<String> nonCompliantChecks, List<String> toCheckChecks,
                                List<String> verifiedChecks) {
        List<AiQuestion> questions = aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId);

        List<AiQuestion> answeredQuestions = questions.stream()
                .filter(q -> "ANSWERED".equals(q.getStatus()))
                .toList();

        String qaSection = IntStream.range(0, answeredQuestions.size())
                .mapToObj(i -> {
                    AiQuestion q = answeredQuestions.get(i);
                    String answerText = aiQuestionAnswerRepository
                            .findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId())
                            .map(AiQuestionAnswer::getAnswerText)
                            .orElse("(sans réponse)");
                    return "Q%d : %s\nR%d : %s".formatted(i + 1, q.getQuestionText(), i + 1, answerText);
                })
                .collect(Collectors.joining("\n"));

        StringBuilder prompt = new StringBuilder();
        prompt.append("[Synthèse précédente]\n").append(previousAnalysisResult).append("\n\n");
        prompt.append("[Questions et réponses de l'avocat]\n")
              .append(qaSection.isEmpty() ? "(aucune réponse)" : qaSection);

        if (chatSummary != null && !chatSummary.isBlank()) {
            prompt.append("\n\n[Échanges libres avec l'assistant — points clés]\n").append(chatSummary);
        }

        if (nonCompliantChecks != null && !nonCompliantChecks.isEmpty()) {
            prompt.append("\n\n[Points procéduraux non conformes]\n");
            nonCompliantChecks.forEach(c -> prompt.append("- ").append(c).append("\n"));
        }

        if (toCheckChecks != null && !toCheckChecks.isEmpty()) {
            prompt.append("\n\n[Points procéduraux à vérifier — non encore qualifiés par l'avocat]\n");
            toCheckChecks.forEach(c -> prompt.append("- ").append(c).append("\n"));
        }

        if (verifiedChecks != null && !verifiedChecks.isEmpty()) {
            prompt.append("\n\n[Points procéduraux vérifiés — à reconsidérer si nécessaire]\n");
            verifiedChecks.forEach(c -> prompt.append("- ").append(c).append("\n"));
        }

        return prompt.toString();
    }
}
