package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseDeadlineService;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.ExtractionStatus;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Profile({"local", "prod"})
public class CaseAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(CaseAnalysisService.class);

    static final String SYSTEM_PROMPT_TEMPLATE = """
            Tu es un assistant juridique expert en %s.
            Tu reçois les analyses de plusieurs documents d'un dossier juridique.
            Produis une synthèse globale du dossier en agrégeant ces analyses.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"timeline": [{"date": "YYYY-MM-DD", "evenement": "..."}], "faits": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "points_juridiques": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "risques": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "questions_ouvertes": [...], "pieces_manquantes": [...], "points_procedure": [...], "score_risque": {"niveau": "FAIBLE"|"MOYEN"|"ELEVE", "valeur": <0-100>}, "delais_detectes": [{"label": "...", "date_detectee": "YYYY-MM-DD", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>"}]}
            Pour les champs "faits", "points_juridiques" et "risques", chaque élément est un objet avec "texte" (le contenu), "source" (nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus) et "extrait" (phrase exacte tirée du document). Si la source n'est pas identifiable, utilise "source": null et "extrait": null.
            F-146 : ajoute AUSSI à chaque item un champ "sourceRef" précisant la pièce juridique exacte, au format {"documentName": "<nom fichier>", "pieceType": "<type pièce parmi la liste du contexte>", "pieceLabel": "<label de la pièce tel qu'indiqué dans la section PIÈCES IDENTIFIÉES>", "pageStart": <page début>, "pageEnd": <page fin>}. Utilise les informations de la section "=== PIÈCES IDENTIFIÉES DANS LES DOCUMENTS ===" fournie dans le prompt utilisateur. Si la pièce n'est pas identifiable ou si le dossier n'a pas de pièces détectées (dossier pré-F-145), utilise "sourceRef": null. Ne jamais inventer un label de pièce qui n'apparaît pas dans la section PIÈCES IDENTIFIÉES.
            La timeline doit lister les événements clés du dossier par ordre chronologique. Si aucune date n'est identifiable, utilise "timeline": [].
            Le champ "pieces_manquantes" liste les pièces habituellement attendues dans ce type de dossier qui sont absentes des documents fournis. Chaque élément est un objet {"texte": "<description de la pièce>", "critere_code": "<code ou null>"}. "critere_code" est rempli UNIQUEMENT si l'absence de cette pièce correspond à un des codes surveillés :
            - Critères F-DT-08 Validité licenciement (droit du travail) : FR_CONVOCATION, FR_ENTRETIEN, FR_DELAI_NOTIFICATION, FR_MOTIVATION, FR_MOTIF_REEL, FR_PROCEDURE_DISCIPLINAIRE, FR_ORDRE_LICENCIEMENT, BE_NOTIFICATION, BE_PREAVIS, BE_MOTIVATION, BE_AUDITION, BE_NON_DISCRIMINATION, BE_PROTECTION_SPECIALE, BE_INDEMNITE_MANIFESTE.
            - Pièces F-FA-07 Checklist divorce (droit de la famille) : FR_ACTE_MARIAGE, FR_ACTE_NAISSANCE_EPOUX, FR_ACTE_NAISSANCE_ENFANTS, FR_LIVRET_FAMILLE, FR_JUSTIF_DOMICILE, FR_CONTRAT_MARIAGE, FR_ETAT_PATRIMOINE, FR_JUSTIF_REVENUS, FR_PIECE_IDENTITE, BE_ACTE_MARIAGE, BE_ACTE_NAISSANCE_EPOUX, BE_ACTE_NAISSANCE_ENFANTS, BE_COMPOSITION_MENAGE, BE_CONTRAT_MARIAGE, BE_CONVENTION_PREALABLE, BE_JUSTIF_REVENUS, BE_PIECE_IDENTITE.
            Sinon null. Exemple : {"texte": "Copie intégrale de l'acte de mariage", "critere_code": "FR_ACTE_MARIAGE"}. Rétrocompat : format string legacy accepté. Si le dossier semble complet, utilise "pieces_manquantes": [].
            Le champ "points_procedure" liste les étapes procédurales légalement requises dans ce type de dossier. Chaque élément est un objet {"texte": "<description de l'étape>", "critere_code": "<code ou null>", "expected_value": "<valeur ou null>"}. "critere_code" est rempli uniquement si le point porte sur l'un des critères surveillés :
            - Critères F-DT-08 Validité licenciement (droit du travail, binaires) : FR_CONVOCATION, FR_ENTRETIEN, FR_DELAI_NOTIFICATION, FR_MOTIVATION, FR_MOTIF_REEL, FR_PROCEDURE_DISCIPLINAIRE, FR_ORDRE_LICENCIEMENT, BE_NOTIFICATION, BE_PREAVIS, BE_MOTIVATION, BE_AUDITION, BE_NON_DISCRIMINATION, BE_PROTECTION_SPECIALE, BE_INDEMNITE_MANIFESTE. Pour ces critères, "expected_value" doit rester null (statut VERIFIED/NON_COMPLIANT porte le signal).
            - Étapes F-FA-07 Checklist divorce (droit de la famille, binaires) : FR_CHOIX_AVOCATS, FR_REDACTION_CONVENTION, FR_ENVOI_LRAR, FR_DELAI_REFLEXION, FR_SIGNATURE_CONVENTION, FR_DEPOT_NOTAIRE, FR_ENREGISTREMENT, BE_CHOIX_AVOCAT, BE_REDACTION_CONVENTION, BE_REQUETE_CONJOINTE, BE_COMPARUTION, BE_JUGEMENT, BE_TRANSCRIPTION. Pour ces étapes, "expected_value" doit rester null. Sémantique : VERIFIED = étape accomplie, NON_COMPLIANT = étape non faite.
            - Critère F-FA-06 Calendrier garde (droit de la famille, énuméré) : FA06_MODE_GARDE. Renseigne obligatoirement "expected_value" avec la valeur affirmée par le point, parmi : ALTERNEE_FR, DVH_CLASSIQUE_FR, DVH_ELARGI_FR (France), ALTERNEE_BE, SECONDAIRE_BE, SECONDAIRE_ELARGI_BE (Belgique). Exemple : {"texte": "Résidence alternée une semaine sur deux actée dans la convention", "critere_code": "FA06_MODE_GARDE", "expected_value": "ALTERNEE_FR"}.
            - Critère F-IM-05 Titre de séjour (droit de l'immigration, énuméré) : IM05_MOTIF. Renseigne obligatoirement "expected_value" avec le motif de la demande, parmi : TRAVAIL, ETUDES, FAMILLE, ASILE, AUTRE. Exemple : {"texte": "La demande est fondée sur un regroupement familial avec conjoint français", "critere_code": "IM05_MOTIF", "expected_value": "FAMILLE"}.
            - Critère F-IM-06 Recours (droit de l'immigration, énuméré) : IM06_RECOURS_TYPE. Renseigne obligatoirement "expected_value" avec le type de recours à former, parmi : RECOURS_GRACIEUX_PREFET, RECOURS_CONTENTIEUX_TA, RECOURS_CNDA (France), RECOURS_CGRA, RECOURS_CCE, RECOURS_CE_BELGIQUE (Belgique). Exemple : {"texte": "Le refus OFPRA doit être contesté devant la CNDA dans un délai de 30 jours", "critere_code": "IM06_RECOURS_TYPE", "expected_value": "RECOURS_CNDA"}.
            - Critère F-IM-07 Droit au travail (droit de l'immigration, énuméré) : IM07_TITRE_TYPE. Renseigne obligatoirement "expected_value" avec le code du titre de séjour parmi les 16 codes (identiques à F-IM-05) : VLS_TS_ETUDIANT, VLS_TS_SALARIE, CST_SALARIE, CARTE_PLURIANNUELLE, CARTE_RESIDENT, APS, CST_VPF, RECEPISSE_ASILE (France), CARTE_A_TRAVAIL, CARTE_A_ETUDES, CARTE_A_FAMILLE, CARTE_B, CARTE_C, PERMIS_UNIQUE, ANNEXE_15, ATTESTATION_IMMATRICULATION (Belgique). Ne renseigner ce critère que si le point évoque spécifiquement le droit au travail attaché à un titre.
            - Critère F-DT-09 Type de rupture (énuméré) : DT09_TYPE_RUPTURE. Pour ce critère, renseigne obligatoirement "expected_value" avec la valeur affirmée par le point, parmi : LICENCIEMENT, LICENCIEMENT_ECONOMIQUE, RUPTURE_CONVENTIONNELLE (France), LICENCIEMENT_ORDINAIRE, RUPTURE_AMIABLE (Belgique). Exemple : {"texte": "Convention de rupture conventionnelle homologuée présente au dossier", "critere_code": "DT09_TYPE_RUPTURE", "expected_value": "RUPTURE_CONVENTIONNELLE"}.
            Pour tout point sans lien avec ces critères, "critere_code" et "expected_value" restent null. Rétrocompat : format string legacy accepté. Si la procédure semble conforme, utilise "points_procedure": [].
            SF-96-06 — Durcissement : quand "critere_code" est null, "points_procedure" ne doit contenir QUE des vérifications binaires factuelles d'étapes légalement requises sur le dossier en cours (les 3 statuts ✅Vérifié / ❌Non conforme / ⚠️À vérifier doivent tous avoir du sens sur l'item). SONT INTERDITS dans "points_procedure" et doivent être redirigés ailleurs : (a) options stratégiques ("En cas de demande…", "Si l'avocat envisage…", "Possibilité de demander…", "Alternative…") → mettre dans "questions_ouvertes" ; (b) opportunités futures à plus de 6 mois ("Après N ans de mariage…", "À partir de…", "Une fois N années révolues…") → "risques" si elles imposent un délai à respecter, sinon "questions_ouvertes" ; (c) recommandations d'action ("Demande à déposer auprès de…", "Joindre la convention…", "Prendre attache avec…") → "questions_ouvertes". Règle de répartition : on VÉRIFIE dans points_procedure, on PROPOSE dans questions_ouvertes, on ALERTE dans risques.
            Le champ "score_risque" est obligatoire : évalue le niveau de risque global du dossier. "niveau" est l'un de "FAIBLE", "MOYEN" ou "ELEVE". "valeur" est un entier entre 0 et 100 reflétant l'intensité du risque (0 = aucun risque, 100 = risque maximum).
            Le champ "delais_detectes" liste les délais légaux détectés dans les documents (ex: délai de recours, délai de prescription). Format : [{"label": "Délai de recours prud'homal", "date_detectee": "YYYY-MM-DD", "source": "<nom exact du fichier>"}]. Si aucun délai détectable, utilise "delais_detectes": [].

            Le champ "source_explanations" liste UNE explication par donnée factuelle clé, pour alimenter le popover d'incohérence (F-IA-03). Chaque explication SÉPARE STRICTEMENT 3 zones affichables :
            - sentence : règle juridique pure (≤ 220 car), SANS mention du nom du document/question/F96/pièce. Exemple CORRECT : "La convention BTP prévoit une prime de 12 %% après 15 ans." Exemple INCORRECT : "Selon contrat_dupont.pdf, la prime est de 12 %%."
            - label : nom CANONIQUE court de la source (nom de fichier exact pour DOCUMENT ; question complète pour QUESTION_AI ; description courte pour CHECKLIST_F96 ; intitulé court pour MISSING_PIECE).
            - secondaryText : citation ou détail verbatim (≤ 200 car). RÉUTILISE verbatim les "extrait" que tu as déjà produits ci-dessus dans faits/points_juridiques/risques. Exemples : "Clause 6.2 — 'Prime d'ancienneté : 12%% après 15 ans'" | "Réponse de l'avocat : '15 ans et 2 mois'" | "Marqué non conforme — 'Aucune LRAR dans les 5 jours'".
            Format : [{"sourceKey": "<snake_case|UPPER_F96_CODE>", "sourceType": "DOCUMENT"|"QUESTION_AI"|"CHECKLIST_F96"|"MISSING_PIECE"|"ANALYSIS_DETECTION", "label": "…", "sentence": "…", "secondaryText": "…", "anchorDocName": "<nom exact doc ou null>"}].
            sourceKeys génériques attendus si la donnée est dans la synthèse : convention_collective, date_entree, salaire_brut_mensuel, conges_contractuels, prime_anciennete_contractuelle, type_rupture, date_licenciement, duree_mariage, revenus_conjoints, nationalite_ue, type_titre_sejour, type_recours, date_notification_decision_contestee. Codes F96 additionnels possibles : FR_CONVOCATION, FR_MOTIVATION, BE_AUDITION, RC_CONSENTEMENT, RC_DELAI_RETRACTATION, DT09_TYPE_RUPTURE, FA05_VALEUR_VENALE, FA06_MODE_GARDE, IM05_MOTIF, IM06_RECOURS_TYPE, IM07_TITRE_TYPE, etc. Produis uniquement les sourcekeys dont la donnée est concrète dans la synthèse ; omet les autres. Aucune invention : un label DOCUMENT doit correspondre à un fichier réellement listé dans le prompt utilisateur. Si aucune source unique n'est identifiable, utilise sourceType="ANALYSIS_DETECTION" et label="Synthèse du dossier". Si aucune donnée factuelle pertinente, "source_explanations": [].
            IMPORTANT : si plusieurs sources corroborent la même donnée (ex. un document ET une réponse à une question confirment la même convention), produis PLUSIEURS entries avec le MÊME sourceKey, chacune avec un sourceType et label différents. Cela permet d'afficher les sources côte à côte. Exemple : [{"sourceKey": "convention_collective", "sourceType": "DOCUMENT", "label": "contrat.pdf", ...}, {"sourceKey": "convention_collective", "sourceType": "QUESTION_AI", "label": "Quelle convention ?", ...}].

            Contraintes de longueur : %d entrées timeline maximum, %d faits maximum, %d points_juridiques maximum, %d risques maximum, %d questions_ouvertes maximum, %d pièces manquantes maximum, %d points procédure maximum. Sois concis.
            """;

    static String buildSystemPrompt(String legalDomain, String country, AnalysisLimitsProperties.LevelLimits limits) {
        return SYSTEM_PROMPT_TEMPLATE.formatted(
                LegalDomainPromptBuilder.domainLabel(legalDomain, country),
                limits.getTimeline(), limits.getFaits(),
                limits.getPointsJuridiques(), limits.getRisques(), limits.getQuestionsOuvertes(),
                limits.getPiecesManquantes(), limits.getPointsProcedure())
                + LegalDomainPromptBuilder.domainSpecificInstruction(legalDomain);
    }

    record PreparedCaseAnalysis(UUID analysisId, String prompt, String systemPrompt, UUID caseFileId,
                                 AnalysisLimitsProperties.LevelLimits limits) {}

    /** SF — budget pour les extraits bruts injectés par doc (pour éviter les
     *  mauvaises classifications quand les doc-analyses ne captent pas le
     *  mécanisme factuel de rupture, ex. Convention de rupture signée vs
     *  requalification en licenciement). 2000 car ≈ ~500 tokens par doc. */
    static final int RAW_DOC_PREFIX_CHARS = 2_000;

    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final DocumentExtractionRepository documentExtractionRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final AnthropicService anthropicService;
    private final AnalysisJobRepository analysisJobRepository;
    private final RabbitTemplate rabbitTemplate;
    private final UsageEventService usageEventService;
    private final ApplicationEventPublisher eventPublisher;
    private final AnalysisDocumentSnapshotService analysisDocumentSnapshotService;
    private final AnalysisLimitsProperties analysisLimitsProperties;
    private final ProcedureCheckService procedureCheckService;
    private final CaseDeadlineService caseDeadlineService;
    private final SourceExplanationGenerator sourceExplanationGenerator;
    private final SourceExplanationService sourceExplanationService;
    private final PiecesPromptContext piecesPromptContext;

    @Lazy @Autowired
    private CaseAnalysisService self;

    public CaseAnalysisService(DocumentAnalysisRepository documentAnalysisRepository,
                               DocumentExtractionRepository documentExtractionRepository,
                               CaseAnalysisRepository caseAnalysisRepository,
                               CaseFileRepository caseFileRepository,
                               AnthropicService anthropicService,
                               AnalysisJobRepository analysisJobRepository,
                               RabbitTemplate rabbitTemplate,
                               UsageEventService usageEventService,
                               ApplicationEventPublisher eventPublisher,
                               AnalysisDocumentSnapshotService analysisDocumentSnapshotService,
                               AnalysisLimitsProperties analysisLimitsProperties,
                               ProcedureCheckService procedureCheckService,
                               CaseDeadlineService caseDeadlineService,
                               SourceExplanationGenerator sourceExplanationGenerator,
                               SourceExplanationService sourceExplanationService,
                               PiecesPromptContext piecesPromptContext) {
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.documentExtractionRepository = documentExtractionRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.anthropicService = anthropicService;
        this.analysisJobRepository = analysisJobRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.usageEventService = usageEventService;
        this.eventPublisher = eventPublisher;
        this.analysisDocumentSnapshotService = analysisDocumentSnapshotService;
        this.analysisLimitsProperties = analysisLimitsProperties;
        this.procedureCheckService = procedureCheckService;
        this.caseDeadlineService = caseDeadlineService;
        this.sourceExplanationGenerator = sourceExplanationGenerator;
        this.sourceExplanationService = sourceExplanationService;
        this.piecesPromptContext = piecesPromptContext;
    }

    @RabbitListener(queues = RabbitMQConfig.CASE_ANALYSIS_QUEUE, concurrency = "3")
    public void consumeCaseAnalysis(CaseAnalysisMessage message) {
        long startMs = System.currentTimeMillis();
        UUID caseFileId = message.caseFileId();

        PreparedCaseAnalysis prepared = self.prepareCaseAnalysis(message);
        if (prepared == null) return;

        AnthropicResult result = null;
        Exception failure = null;
        try {
            log.info("Case analysis START for caseFile {} ({} chars)", caseFileId, prepared.prompt().length());
            long anthropicStart = System.currentTimeMillis();
            // F-146/F-148 : l'injection de PiecesPromptContext + descriptions Vision
            // fait gonfler la sortie attendue (11 pièces × sourceRef + éventuelles
            // visual_description → facilement 8 000+ tokens de JSON). On aligne sur
            // EnrichedAnalysisService (16384) pour éviter la troncature silencieuse
            // constatée en staging 2026-04-23 (dossier E-35).
            // F-142-04 : prompt caching ephemeral — le system prompt (plusieurs milliers
            // de tokens : domaine, limites, instruction, PiecesPromptContext) est
            // réutilisé entre appels successifs (re-analyse, question chat). Gain ~85 %
            // de latence prefill sur les appels dans la fenêtre de 5 min.
            result = anthropicService.analyzeWithSystemCache(prepared.systemPrompt(), prepared.prompt(), 16384);
            long anthropicMs = System.currentTimeMillis() - anthropicStart;
            log.info("Case analysis DONE for caseFile {} — Anthropic {}ms, total {}ms, tokens {}/{}",
                    caseFileId, anthropicMs, System.currentTimeMillis() - startMs,
                    result.promptTokens(), result.completionTokens());
        } catch (Exception e) {
            log.error("Case analysis FAILED for caseFile {} (total {}ms)", caseFileId,
                    System.currentTimeMillis() - startMs, e);
            failure = e;
        }

        self.finalizeCaseAnalysis(prepared.analysisId(), prepared.caseFileId(), result, failure, prepared.limits());
    }

    @Transactional
    public PreparedCaseAnalysis prepareCaseAnalysis(CaseAnalysisMessage message) {
        UUID caseFileId = message.caseFileId();

        List<DocumentAnalysis> documentAnalyses = documentAnalysisRepository
                .findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE);

        if (documentAnalyses.isEmpty()) {
            log.warn("No DONE document analyses found for caseFile {} — case analysis skipped", caseFileId);
            return null;
        }

        CaseFile caseFile = caseFileRepository.findById(caseFileId).orElse(null);
        if (caseFile == null) {
            log.error("CaseFile {} not found — case analysis skipped", caseFileId);
            return null;
        }

        AnalysisJob job = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.CASE_ANALYSIS);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setStatus(AnalysisStatus.PROCESSING);
        job.setTotalItems(1);
        analysisJobRepository.save(job);

        int nextVersion = caseAnalysisRepository.findMaxVersionByCaseFileId(caseFileId) + 1;

        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);
        analysis.setVersion(nextVersion);
        analysis.setAnalysisType(AnalysisType.STANDARD);
        analysis.setAnalysisStatus(AnalysisStatus.PENDING);
        analysis = caseAnalysisRepository.save(analysis);
        analysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        analysis = caseAnalysisRepository.save(analysis);

        analysisDocumentSnapshotService.snapshot(analysis.getId(), caseFile);

        fr.ailegalcase.workspace.Workspace ws = caseFile.getWorkspace();
        String legalDomain = ws != null ? ws.getLegalDomain() : "DROIT_DU_TRAVAIL";
        String country     = ws != null ? ws.getCountry()     : "FRANCE";
        AnalysisLimitsProperties.LevelLimits limits = analysisLimitsProperties.forDomain(legalDomain).getDossier();
        String systemPrompt = buildSystemPrompt(legalDomain, country, limits);
        // F-146 SF-146-01 : préfixe le prompt utilisateur avec la liste des pièces
        // identifiées (F-145) pour que l'IA puisse produire des `sourceRef` précis.
        String piecesContext = piecesPromptContext.buildContextForCaseFile(caseFileId);
        String userPrompt = (piecesContext == null || piecesContext.isEmpty())
                ? buildAggregatedPrompt(documentAnalyses)
                : piecesContext + "\n" + buildAggregatedPrompt(documentAnalyses);
        return new PreparedCaseAnalysis(analysis.getId(), userPrompt, systemPrompt, caseFileId, limits);
    }

    @Transactional
    public void finalizeCaseAnalysis(UUID analysisId, UUID caseFileId, AnthropicResult result, Exception failure,
                                      AnalysisLimitsProperties.LevelLimits limits) {
        CaseAnalysis analysis = caseAnalysisRepository.findById(analysisId).orElseThrow();

        if (failure != null) {
            analysis.setAnalysisStatus(AnalysisStatus.FAILED);
        } else {
            String truncated = AnalysisJsonTruncator.truncateCaseAnalysis(result.content(), limits);
            analysis.setAnalysisResult(truncated);
            analysis.setModelUsed(result.modelUsed());
            analysis.setPromptTokens(result.promptTokens());
            analysis.setCompletionTokens(result.completionTokens());
            analysis.setAnalysisStatus(AnalysisStatus.DONE);
            CaseAnalysisResponse.populateCounts(analysis, truncated);
            CaseAnalysisResponse.populateRiskScore(analysis, truncated);
        }
        caseAnalysisRepository.save(analysis);

        if (failure == null) {
            procedureCheckService.createChecks(analysis, analysis.getAnalysisResult());
            try {
                caseDeadlineService.createAiDetectedDeadlines(analysis, analysis.getAnalysisResult());
            } catch (Exception e) {
                log.warn("Fail-open: AI deadline detection failed for analysis {}: {}", analysis.getId(), e.getMessage());
            }
            // SF-IA-03-15a/17 : génération des phrases d'explication par source via Haiku (synchrone, fail-open).
            try {
                caseFileRepository.findById(caseFileId).ifPresent(cf -> {
                    List<SourceExplanationData> explanations = sourceExplanationGenerator.generate(cf, analysis);
                    sourceExplanationService.persist(analysis, explanations);
                });
            } catch (Exception e) {
                log.warn("Fail-open: source explanation generation failed for analysis {}: {}",
                        analysis.getId(), e.getMessage());
            }
        }

        AnalysisJob job = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.CASE_ANALYSIS);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setProcessedItems(1);
        job.setStatus(analysis.getAnalysisStatus());
        if (analysis.getAnalysisStatus() == AnalysisStatus.FAILED) {
            job.setErrorMessage("Case analysis failed");
            reportJobFailureToSentry(caseFileId, JobType.CASE_ANALYSIS, "Case analysis failed");
        }
        analysisJobRepository.save(job);

        AnalysisStatus finalStatus = analysis.getAnalysisStatus();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new AnalysisStatusEvent(caseFileId, finalStatus, JobType.CASE_ANALYSIS));
                if (finalStatus == AnalysisStatus.DONE) {
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.AI_QUESTION_GENERATION_EXCHANGE,
                            RabbitMQConfig.AI_QUESTION_GENERATION_ROUTING_KEY,
                            new AiQuestionGenerationMessage(caseFileId));
                }
            }
        });

        if (finalStatus == AnalysisStatus.DONE) {
            int promptTokens = analysis.getPromptTokens();
            int completionTokens = analysis.getCompletionTokens();
            caseFileRepository.findCreatedByUserIdById(caseFileId).ifPresent(userId ->
                usageEventService.record(caseFileId, userId, JobType.CASE_ANALYSIS,
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

    private String buildAggregatedPrompt(List<DocumentAnalysis> documentAnalyses) {
        List<DocumentAnalysis> sorted = documentAnalyses.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .toList();

        // Charge les extractions en batch pour récupérer le texte brut (prefix).
        // Permet à Claude de voir directement "Convention de rupture signée le..."
        // au lieu de s'appuyer uniquement sur les faits pré-extraits qui peuvent
        // être biaisés par les arguments de requalification.
        List<UUID> docIds = sorted.stream().map(da -> da.getDocument().getId()).toList();
        Map<UUID, DocumentExtraction> extractionsByDocId = documentExtractionRepository
                .findByDocumentIdIn(docIds).stream()
                .collect(Collectors.toMap(e -> e.getDocument().getId(), e -> e, (a, b) -> a, HashMap::new));

        return IntStream.range(0, sorted.size())
                .mapToObj(i -> {
                    DocumentAnalysis da = sorted.get(i);
                    String filename = da.getDocument().getOriginalFilename();
                    String label = filename != null ? filename : "document-%d".formatted(i);

                    String rawPrefix = "";
                    DocumentExtraction ex = extractionsByDocId.get(da.getDocument().getId());
                    if (ex != null && ex.getExtractionStatus() == ExtractionStatus.DONE
                            && ex.getExtractedText() != null && !ex.getExtractedText().isBlank()) {
                        String text = ex.getExtractedText();
                        String slice = text.length() <= RAW_DOC_PREFIX_CHARS
                                ? text : text.substring(0, RAW_DOC_PREFIX_CHARS) + " [...]";
                        rawPrefix = "\n[Extrait du document brut] " + slice.replace("\n", " ").trim() + "\n";
                    }

                    return "%s : %s%s".formatted(label, da.getAnalysisResult(), rawPrefix);
                })
                .collect(Collectors.joining("\n"));
    }
}
