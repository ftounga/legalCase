package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Profile({"local", "prod"})
public class AiQuestionService {

    private static final Logger log = LoggerFactory.getLogger(AiQuestionService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static final String SYSTEM_PROMPT_TEMPLATE = """
            Tu es un assistant juridique expert en %s.
            Tu reçois la synthèse globale d'un dossier juridique.
            Génère une liste de questions complémentaires pour l'avocat afin d'approfondir l'analyse.
            Ces questions doivent porter sur des éléments manquants, des ambiguïtés ou des points à clarifier.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"questions": [{"texte": "Question 1 ?", "critere_code": "<code ou null>", "expected_value": "<valeur ou null>"}, ...]}
            Chaque question est un objet. "critere_code" est rempli UNIQUEMENT si la question porte sur un critère surveillé :
            - Critères F-DT-08 Validité licenciement (droit du travail, binaires) : FR_CONVOCATION, FR_ENTRETIEN, FR_DELAI_NOTIFICATION, FR_MOTIVATION, FR_MOTIF_REEL, FR_PROCEDURE_DISCIPLINAIRE, FR_ORDRE_LICENCIEMENT, BE_NOTIFICATION, BE_PREAVIS, BE_MOTIVATION, BE_AUDITION, BE_NON_DISCRIMINATION, BE_PROTECTION_SPECIALE, BE_INDEMNITE_MANIFESTE. "expected_value" reste null. Une réponse "oui" = critère respecté.
            - Étapes F-FA-07 Checklist divorce (droit de la famille, binaires) : FR_CHOIX_AVOCATS, FR_REDACTION_CONVENTION, FR_ENVOI_LRAR, FR_DELAI_REFLEXION, FR_SIGNATURE_CONVENTION, FR_DEPOT_NOTAIRE, FR_ENREGISTREMENT, BE_CHOIX_AVOCAT, BE_REDACTION_CONVENTION, BE_REQUETE_CONJOINTE, BE_COMPARUTION, BE_JUGEMENT, BE_TRANSCRIPTION. "expected_value" reste null. Une réponse "oui" = étape accomplie.
            - Pièces F-FA-07 Checklist divorce (droit de la famille, binaires) : FR_ACTE_MARIAGE, FR_ACTE_NAISSANCE_EPOUX, FR_ACTE_NAISSANCE_ENFANTS, FR_LIVRET_FAMILLE, FR_JUSTIF_DOMICILE, FR_CONTRAT_MARIAGE, FR_ETAT_PATRIMOINE, FR_JUSTIF_REVENUS, FR_PIECE_IDENTITE, BE_ACTE_MARIAGE, BE_ACTE_NAISSANCE_EPOUX, BE_ACTE_NAISSANCE_ENFANTS, BE_COMPOSITION_MENAGE, BE_CONTRAT_MARIAGE, BE_CONVENTION_PREALABLE, BE_JUSTIF_REVENUS, BE_PIECE_IDENTITE. "expected_value" reste null. Une réponse "oui" = pièce présente.
            - Critère F-FA-06 Calendrier garde (droit de la famille, énuméré) : FA06_MODE_GARDE. "expected_value" obligatoire parmi : ALTERNEE_FR, DVH_CLASSIQUE_FR, DVH_ELARGI_FR (France), ALTERNEE_BE, SECONDAIRE_BE, SECONDAIRE_ELARGI_BE (Belgique). Une réponse "oui" confirme le mode indiqué par "expected_value". Exemple : {"texte": "Une résidence alternée une semaine sur deux est-elle mise en place ?", "critere_code": "FA06_MODE_GARDE", "expected_value": "ALTERNEE_FR"}.
            - Critère F-IM-05 Titre de séjour (droit de l'immigration, énuméré) : IM05_MOTIF. "expected_value" obligatoire parmi : TRAVAIL, ETUDES, FAMILLE, ASILE, AUTRE. Une réponse "oui" confirme le motif indiqué. Exemple : {"texte": "La demande est-elle fondée sur un motif de regroupement familial ?", "critere_code": "IM05_MOTIF", "expected_value": "FAMILLE"}.
            - Critère F-IM-06 Recours (droit de l'immigration, énuméré) : IM06_RECOURS_TYPE. "expected_value" obligatoire parmi : RECOURS_GRACIEUX_PREFET, RECOURS_CONTENTIEUX_TA, RECOURS_CNDA (France), RECOURS_CGRA, RECOURS_CCE, RECOURS_CE_BELGIQUE (Belgique). Une réponse "oui" confirme le type de recours. Exemple : {"texte": "Le recours à former est-il un recours contentieux devant le tribunal administratif ?", "critere_code": "IM06_RECOURS_TYPE", "expected_value": "RECOURS_CONTENTIEUX_TA"}.
            - Critère F-IM-07 Droit au travail (droit de l'immigration, énuméré) : IM07_TITRE_TYPE. "expected_value" obligatoire parmi les 16 codes de titre (identiques à F-IM-05). Une réponse "oui" confirme le code de titre.
            - Critère F-DT-09 Type de rupture (énuméré) : DT09_TYPE_RUPTURE. Renseigne obligatoirement "expected_value" avec la valeur confirmée par une réponse "oui", parmi : LICENCIEMENT, LICENCIEMENT_ECONOMIQUE, RUPTURE_CONVENTIONNELLE (France), LICENCIEMENT_ORDINAIRE, RUPTURE_AMIABLE (Belgique). Exemple : {"texte": "Une convention de rupture conventionnelle a-t-elle été homologuée ?", "critere_code": "DT09_TYPE_RUPTURE", "expected_value": "RUPTURE_CONVENTIONNELLE"} → une réponse "oui" confirme ce type.
            - Critères F-DT-36 Nullité de procédure de licenciement (droit du travail, FRANCE UNIQUEMENT, binaires) : DT36_DATE_ENTRETIEN (la date de l'entretien préalable a-t-elle été identifiée dans les pièces ?), DT36_MOTIVATION (la lettre de licenciement énonce-t-elle un motif précis et matériellement vérifiable ?), DT36_ENTRETIEN_TENU (l'entretien préalable a-t-il effectivement eu lieu selon les pièces ?). "expected_value" reste null. Une réponse "oui" = critère respecté/documenté.
            - Critère F-DT-11 Harcèlement / licenciement nul (droit du travail, FRANCE UNIQUEMENT, binaire) : HLN_MOTIF_NULLITE (un motif de nullité du licenciement est-il identifié dans les pièces : harcèlement moral/sexuel, discrimination, protection représentant du personnel, maternité/paternité, accident du travail ?). "expected_value" reste null. Une réponse "oui" = motif de nullité documenté.
            - Critères F-DT-13 Licenciement économique (droit du travail, FRANCE UNIQUEMENT, binaires) : DT13_MOTIF_ECONOMIQUE (un motif économique justificatif est-il documenté dans les pièces : difficultés économiques, mutation technologique, sauvegarde de compétitivité, cessation d'activité ?), DT13_DATE_NOTIFICATION (la date de notification du licenciement économique est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = présence documentée.
            - Critère F-DT-14 Plan de Sauvegarde de l'Emploi (droit du travail, FRANCE UNIQUEMENT, binaire) : PSE_DATE_PROJET (la date de présentation du projet de PSE est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = date documentée.
            - Critère F-DT-30 Protection représentant du personnel (droit du travail, FRANCE UNIQUEMENT, binaire) : PROTECTION_RP_MOTIF (le motif invoqué à l'appui du licenciement d'un représentant du personnel est-il identifié dans les pièces ?). "expected_value" reste null. Une réponse "oui" = motif documenté.
            - Critères F-DT-10 Validité rupture conventionnelle (droit du travail, FRANCE UNIQUEMENT, binaires) : RC_CONSENTEMENT (le consentement des deux parties est-il libre et éclairé — absence de vice du consentement ?), RC_DELAI_RETRACTATION (le délai de rétractation de 15 jours calendaires a-t-il été respecté ?), RC_HOMOLOGATION (l'homologation par la DREETS/DRIESST est-elle obtenue ou en cours ?), RC_ASSISTANCE (le droit à l'assistance lors de l'entretien a-t-il été respecté ?), RC_INDEMNITE (l'indemnité spécifique est-elle au moins égale au plancher légal : 1/4 de mois par année d'ancienneté ?), RC_ENTRETIENS (au moins un entretien préalable entre les parties a-t-il été tenu ?). "expected_value" reste null. Une réponse "oui" = critère respecté.
            - Critère F-DT-22 Requalification CDD en CDI (droit du travail, FRANCE UNIQUEMENT, binaire) : DT22_SALAIRE (le salaire brut mensuel de référence est-il identifié dans les pièces ?). "expected_value" reste null. Une réponse "oui" = salaire identifié.
            - Critère F-DT-23 Requalification contrat intérim en CDI (droit du travail, FRANCE UNIQUEMENT, binaire) : DT23_SALAIRE (le salaire brut mensuel de référence est-il identifié dans les pièces ?). "expected_value" reste null. Une réponse "oui" = salaire identifié.
            - Critère F-DT-24 Clause de non-concurrence (droit du travail, FRANCE UNIQUEMENT, binaire) : DT24_SALAIRE (le salaire brut mensuel de référence est-il identifié dans les pièces — base de calcul de la contrepartie pécuniaire ?). "expected_value" reste null. Une réponse "oui" = salaire identifié.
            - Critères F-DT-31 Transaction (droit du travail, FRANCE UNIQUEMENT, binaires) : DT31_SALAIRE_MENSUEL (le salaire mensuel brut de référence est-il identifié dans les pièces ?), DT31_ANCIENNETE (l'ancienneté en mois/années est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = donnée identifiée.
            - Critères F-132 Indemnité rupture conventionnelle (droit du travail, FRANCE UNIQUEMENT, binaires) : RCI_SALAIRE (le salaire brut mensuel de référence est-il identifié dans les pièces ?), RCI_ANCIENNETE (l'ancienneté est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = donnée identifiée.
            - Critères F-DT-15 Inaptitude (droit du travail, FRANCE UNIQUEMENT, binaires) : INAPT_ORIGINE (l'origine de l'inaptitude est-elle identifiée dans les pièces : professionnelle — AT/MP — ou non-professionnelle ?), INAPT_RECLASSEMENT (les démarches de reclassement sont-elles documentées dans les pièces ?). "expected_value" reste null. Une réponse "oui" = donnée documentée.
            - Critère F-DT-33 Accident du travail / Maladie professionnelle (droit du travail, FRANCE UNIQUEMENT, binaire) : AT_MP_DATE_ACCIDENT (la date de l'accident du travail ou de la déclaration de maladie professionnelle est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = date identifiée.
            - Critères F-DT-29 Crédit-temps (droit du travail, BELGIQUE UNIQUEMENT, binaires) : CREDIT_TEMPS_ANCIENNETE (l'ancienneté du travailleur est-elle identifiée dans les pièces — condition d'éligibilité au crédit-temps belge ?), CREDIT_TEMPS_AGE (l'âge du travailleur est-il identifié dans les pièces — déterminant pour le régime crédit-temps senior ?). "expected_value" reste null. Une réponse "oui" = donnée documentée.
            - Critère F-136 Type de procédure de travail (droit du travail, binaire) : TRAVAIL_PROCEDURE_TYPE (le type de procédure de travail est-il identifié dans les pièces : licenciement collectif, restructuration, fermeture d'entreprise ?). "expected_value" reste null. Une réponse "oui" = type documenté.
            - Critères F-IM-08 OQTF (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM08_MOTIF_OQTF (le motif de l'OQTF est-il identifié dans les pièces : absence de titre, menace ordre public, refus de séjour ?), IM08_RECOURS_FORME (la forme du recours contre l'OQTF est-elle identifiée : recours gracieux préfet, recours contentieux TA ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critère F-IM-08 Référés administratifs (droit de l'immigration, FRANCE UNIQUEMENT, binaire) : IM08RA_DECISION_CONTESTEE (la décision administrative contestée est-elle identifiée dans les pièces : OQTF, refus de titre, arrêté préfectoral ?). "expected_value" reste null. Une réponse "oui" = décision documentée.
            - Critères F-IM-09 AES Métiers en tension (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM09_DATE_ENTREE_FRANCE (la date d'entrée en France est-elle identifiée dans les pièces ?), IM09_MOIS_ACTIVITE (le nombre de mois d'activité professionnelle est-il identifié dans les pièces — condition AES métiers en tension ?). "expected_value" reste null. Une réponse "oui" = donnée documentée.
            - Critères F-IM-09 AES Étudiant (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM09_ETU_DATE_ENTREE_FRANCE (la date d'entrée en France de l'étudiant est-elle identifiée dans les pièces ?), IM09_ETU_DUREE_PRESENCE (la durée de présence continue en France est-elle identifiée dans les pièces ?), IM09_ETU_DATE_DEPOT_DEMANDE (la date de dépôt de la demande AES étudiant est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = donnée documentée.
            - Critères F-IM-09 AES Humanitaire (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM09H_DATE_ENTREE_FRANCE (la date d'entrée en France est-elle identifiée dans les pièces ?), IM09H_MOTIF_HUMANITAIRE (le motif humanitaire est-il identifié dans les pièces : circonstances humanitaires exceptionnelles, liens personnels et familiaux en France ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            CONVENTION IMPÉRATIVE : toute question avec "critere_code" doit être formulée pour qu'une réponse "oui" porte un signal positif (critère respecté ou type confirmé).
            Rétrocompat acceptée : format string legacy. Génère entre 3 et 8 questions.
            """;

    static String buildSystemPrompt(String legalDomain, String country) {
        return SYSTEM_PROMPT_TEMPLATE.formatted(LegalDomainPromptBuilder.domainLabel(legalDomain, country));
    }

    record PreparedQuestionGeneration(String prompt, String systemPrompt, UUID caseFileId, UUID caseAnalysisId) {}

    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final AiQuestionRepository aiQuestionRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final AnthropicService anthropicService;
    private final UsageEventService usageEventService;
    private final ApplicationEventPublisher eventPublisher;

    @Lazy @Autowired
    private AiQuestionService self;

    public AiQuestionService(CaseAnalysisRepository caseAnalysisRepository,
                             CaseFileRepository caseFileRepository,
                             AiQuestionRepository aiQuestionRepository,
                             AnalysisJobRepository analysisJobRepository,
                             AnthropicService anthropicService,
                             UsageEventService usageEventService,
                             ApplicationEventPublisher eventPublisher) {
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.aiQuestionRepository = aiQuestionRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.anthropicService = anthropicService;
        this.usageEventService = usageEventService;
        this.eventPublisher = eventPublisher;
    }

    @RabbitListener(queues = RabbitMQConfig.AI_QUESTION_GENERATION_QUEUE, concurrency = "3")
    public void consumeQuestionGeneration(AiQuestionGenerationMessage message) {
        long startMs = System.currentTimeMillis();
        UUID caseFileId = message.caseFileId();

        PreparedQuestionGeneration prepared = self.prepareQuestionGeneration(message);
        if (prepared == null) return;

        AnthropicResult result = null;
        Exception failure = null;
        try {
            log.info("Question generation START for caseFile {} ({} chars)", caseFileId, prepared.prompt().length());
            long anthropicStart = System.currentTimeMillis();
            // 8192 tokens = max output Sonnet 4.6. Largement suffisant pour 5-8 questions
            // avec justifications sur n'importe quel dossier. Anthropic facture sur
            // l'usage réel (~1000-2000 tokens typiquement), pas le max → zéro surcoût.
            // Aligne avec CaseAnalysisService et EnrichedAnalysisService. Historique :
            // 1024 → 4096 (2026-04-19 PR #386) → 8192 (cette PR, demande user pour
            // éliminer définitivement le risque de troncature silencieuse).
            // F-142-04 : prompt caching ephemeral (5 min TTL) — gain de latence sur
            // les appels successifs avec le même system prompt dans la même session.
            result = anthropicService.analyzeWithSystemCache(prepared.systemPrompt(), prepared.prompt(), 8192);
            long anthropicMs = System.currentTimeMillis() - anthropicStart;
            log.info("Question generation DONE for caseFile {} — Anthropic {}ms, total {}ms, tokens {}/{}",
                    caseFileId, anthropicMs, System.currentTimeMillis() - startMs,
                    result.promptTokens(), result.completionTokens());
        } catch (Exception e) {
            log.error("Question generation FAILED for caseFile {} (total {}ms)", caseFileId,
                    System.currentTimeMillis() - startMs, e);
            failure = e;
        }

        self.finalizeQuestionGeneration(prepared.caseFileId(), prepared.caseAnalysisId(), result, failure);
    }

    @Transactional
    public PreparedQuestionGeneration prepareQuestionGeneration(AiQuestionGenerationMessage message) {
        UUID caseFileId = message.caseFileId();

        CaseAnalysis caseAnalysis = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .orElse(null);

        if (caseAnalysis == null) {
            log.warn("No DONE case analysis found for caseFile {} — question generation skipped", caseFileId);
            return null;
        }

        CaseFile caseFile = caseFileRepository.findById(caseFileId).orElse(null);
        if (caseFile == null) {
            log.error("CaseFile {} not found — question generation skipped", caseFileId);
            return null;
        }

        AnalysisJob job = analysisJobRepository
                .findByCaseFileIdAndJobType(caseFileId, JobType.QUESTION_GENERATION)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.QUESTION_GENERATION);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setStatus(AnalysisStatus.PROCESSING);
        job.setTotalItems(1);
        analysisJobRepository.save(job);

        fr.ailegalcase.workspace.Workspace ws = caseFile.getWorkspace();
        String systemPrompt = buildSystemPrompt(
                ws != null ? ws.getLegalDomain() : "DROIT_DU_TRAVAIL",
                ws != null ? ws.getCountry() : "FRANCE");
        return new PreparedQuestionGeneration(caseAnalysis.getAnalysisResult(), systemPrompt, caseFileId, caseAnalysis.getId());
    }

    @Transactional
    public void finalizeQuestionGeneration(UUID caseFileId, UUID caseAnalysisId, AnthropicResult result, Exception failure) {
        AnalysisJob job = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.QUESTION_GENERATION)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.QUESTION_GENERATION);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    return j;
                });

        if (failure != null) {
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("Question generation failed");
            analysisJobRepository.save(job);
            // F-185 SF-185-02 — émet aussi l'event SSE FAILED pour que le front masque le spinner.
            publishStatusEventAfterCommit(caseFileId, AnalysisStatus.FAILED);
            return;
        }

        try {
            List<ParsedQuestion> questions = parseQuestions(result.content());
            CaseFile caseFile = caseFileRepository.findById(caseFileId).orElseThrow();
            CaseAnalysis caseAnalysis = caseAnalysisRepository.findById(caseAnalysisId).orElseThrow();

            for (int i = 0; i < questions.size(); i++) {
                ParsedQuestion pq = questions.get(i);
                AiQuestion question = new AiQuestion();
                question.setCaseFile(caseFile);
                question.setCaseAnalysis(caseAnalysis);
                question.setQuestionText(pq.text());
                question.setCritereCode(pq.critereCode());
                question.setExpectedValue(pq.expectedValue());
                question.setOrderIndex(i);
                aiQuestionRepository.save(question);
            }

            job.setProcessedItems(1);
            job.setStatus(AnalysisStatus.DONE);
            log.info("Question generation finalized for caseFile {} — {} questions", caseFileId, questions.size());
        } catch (Exception e) {
            log.error("Question generation finalization FAILED for caseFile {}", caseFileId, e);
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("Question generation failed");
        }

        analysisJobRepository.save(job);

        if (job.getStatus() == AnalysisStatus.DONE) {
            final AnthropicResult finalResult = result;
            caseFileRepository.findCreatedByUserIdById(caseFileId).ifPresent(userId ->
                usageEventService.record(caseFileId, userId, JobType.QUESTION_GENERATION,
                        finalResult.promptTokens(), finalResult.completionTokens()));
        }

        // F-185 SF-185-02 — émission SSE QUESTION_GENERATION_DONE / _FAILED après commit
        // pour que le frontend bascule du spinner "Génération des questions complémentaires…"
        // vers la bannière "N question(s) en attente" sans attendre le polling jobs (5-15 s économisés).
        publishStatusEventAfterCommit(caseFileId, job.getStatus());
    }

    /**
     * F-185 SF-185-02 — helper pour publier l'événement SSE QUESTION_GENERATION
     * après commit Spring. Pas de noop si une transaction n'est pas active : le code
     * s'exécute hors transaction (test unitaire qui ne wrap pas explicitement) → on
     * publie directement.
     */
    private void publishStatusEventAfterCommit(UUID caseFileId, AnalysisStatus status) {
        AnalysisStatusEvent event = new AnalysisStatusEvent(caseFileId, status, JobType.QUESTION_GENERATION);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishEvent(event);
                }
            });
        } else {
            eventPublisher.publishEvent(event);
        }
    }

    record ParsedQuestion(String text, String critereCode, String expectedValue) {}

    static List<ParsedQuestion> parseQuestions(String json) {
        try {
            JsonNode root = MAPPER.readTree(CaseAnalysisResponse.stripMarkdownCodeBlock(json));
            JsonNode questionsNode = root.get("questions");
            if (questionsNode == null || !questionsNode.isArray()) return List.of();
            List<ParsedQuestion> result = new ArrayList<>();
            for (JsonNode item : questionsNode) {
                if (item.isTextual()) {
                    String txt = item.asText();
                    if (txt != null && !txt.isBlank()) result.add(new ParsedQuestion(txt, null, null));
                } else if (item.isObject()) {
                    JsonNode texteNode = item.get("texte");
                    if (texteNode == null || !texteNode.isTextual()) continue;
                    String texte = texteNode.asText();
                    if (texte.isBlank()) continue;
                    String code = null;
                    JsonNode codeNode = item.get("critere_code");
                    if (codeNode != null && codeNode.isTextual()) {
                        String raw = codeNode.asText().trim();
                        if (!raw.isEmpty()) code = raw.toUpperCase();
                    }
                    String expectedValue = null;
                    JsonNode evNode = item.get("expected_value");
                    if (evNode != null && evNode.isTextual()) {
                        String raw = evNode.asText().trim();
                        if (!raw.isEmpty()) expectedValue = raw.toUpperCase();
                    }
                    result.add(new ParsedQuestion(texte, code, expectedValue));
                }
            }
            return List.copyOf(result);
        } catch (Exception e) {
            return List.of();
        }
    }
}
