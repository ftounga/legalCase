package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.document.ExtractionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * F-179 SF-179-01 — détecte les références jurisprudentielles citées dans les
 * documents uploadés d'un dossier, les fait vérifier par Claude Sonnet (existence
 * réelle + fidélité de la position alléguée), et persiste les résultats dans
 * {@link JurisprudenceCheck}.
 *
 * <p>Appelé en post-traitement <strong>fail-open</strong> de
 * {@code CaseAnalysisService.finalizeCaseAnalysis} — pattern miroir de
 * {@code SourceExplanationGenerator} / {@code procedureCheckService}. Toute
 * exception est journalisée sans propagation : l'analyse du dossier reste
 * {@code DONE}.</p>
 *
 * <p>Le fallback web search Légifrance / Juridat (SF-179-02) enrichira a
 * posteriori les checks {@code UNCERTAIN} / {@code NOT_FOUND} peu confiants —
 * il n'est pas implémenté dans cette SF.</p>
 */
@Service
public class JurisprudenceVerificationService {

    private static final Logger log = LoggerFactory.getLogger(JurisprudenceVerificationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Budget de texte brut soumis à Sonnet par document (maîtrise du coût IA). */
    static final int MAX_DOC_TEXT_CHARS = 6_000;

    /** Plafond de documents soumis dans un même prompt (garde-fou taille). */
    static final int MAX_DOCUMENTS = 25;

    /** Budget de tokens de sortie pour la réponse de vérification. */
    static final int VERIFICATION_MAX_TOKENS = 4_000;

    static final String SYSTEM_PROMPT = """
            Tu es un assistant juridique expert chargé de vérifier les références
            jurisprudentielles citées dans des documents (typiquement les conclusions
            de la partie adverse) d'un dossier juridique français ou belge.

            Pour chaque document fourni, tu reçois :
            - le nom du document ;
            - une liste de références candidates pré-détectées par expressions régulières
              (indices — ni exhaustifs, ni forcément corrects) ;
            - un extrait du texte brut du document.

            Ta mission, pour CHAQUE référence jurisprudentielle réellement citée dans le
            texte (qu'elle soit dans la liste candidate OU repérée par toi dans le texte) :
            1. Vérifier l'EXISTENCE RÉELLE de l'arrêt / de la décision (Cour de cassation
               FR ou BE, Conseil d'État FR ou BE, Cour constitutionnelle BE, Conseil
               constitutionnel FR, cours d'appel, tribunaux du travail BE…).
            2. Vérifier la FIDÉLITÉ de la position juridique que le document prête à cet
               arrêt par rapport à son contenu réel.

            Tu renvoies UNIQUEMENT un objet JSON valide, sans texte avant ni après, au
            format : {"checks": [{"documentName": "<nom exact du document>",
            "reference": "<libellé canonique de l'arrêt>", "statut": "<STATUT>",
            "explication": "<explication courte du verdict>",
            "positionAlleguee": "<position que le document prête à l'arrêt, ou null>",
            "claudeConfidence": "HIGH"|"MEDIUM"|"LOW"}]}.

            Les 4 STATUTS possibles, et EUX SEULS :
            - "VERIFIED" : l'arrêt existe ET la position alléguée est fidèle à son contenu.
            - "SUSPECT" : l'arrêt existe MAIS la position alléguée est incohérente avec
              son contenu réel (citation détournée / de mauvaise foi). Renseigne alors
              "positionAlleguee" et explique l'écart dans "explication".
            - "NOT_FOUND" : la référence est introuvable / n'existe pas (probable
              hallucination). N'utilise "NOT_FOUND" que si tu es CERTAIN — confiance HIGH.
            - "UNCERTAIN" : tu n'as pas une connaissance suffisante de cet arrêt pour
              conclure (arrêt obscur, cour d'appel, décision récente). Préfère TOUJOURS
              "UNCERTAIN" plutôt qu'un "NOT_FOUND" non certain : un faux "NOT_FOUND"
              discréditerait l'outil.

            Règles impératives :
            - N'invente JAMAIS de jurisprudence : ne renvoie que des références
              réellement présentes dans les documents fournis.
            - "claudeConfidence" reflète ta certitude sur le verdict (HIGH/MEDIUM/LOW).
            - "documentName" doit correspondre EXACTEMENT au nom d'un document fourni.
            - Si un document ne cite aucune jurisprudence, ne produis aucune entrée pour
              lui. Si aucun document ne cite de jurisprudence, renvoie {"checks": []}.
            """;

    /** Plafond de recherches web par dossier (garde-fou coût / rate limit). */
    static final int DEFAULT_MAX_WEB_SEARCHES_PER_CASE = 10;

    private final DocumentRepository documentRepository;
    private final DocumentExtractionRepository documentExtractionRepository;
    private final JurisprudenceCheckRepository jurisprudenceCheckRepository;
    private final AnthropicService anthropicService;
    private final WebSearchService webSearchService;
    private final int maxWebSearchesPerCase;

    public JurisprudenceVerificationService(
            DocumentRepository documentRepository,
            DocumentExtractionRepository documentExtractionRepository,
            JurisprudenceCheckRepository jurisprudenceCheckRepository,
            AnthropicService anthropicService,
            WebSearchService webSearchService,
            @org.springframework.beans.factory.annotation.Value(
                    "${jurisprudence.web-search.max-searches-per-case:10}") int maxWebSearchesPerCase) {
        this.documentRepository = documentRepository;
        this.documentExtractionRepository = documentExtractionRepository;
        this.jurisprudenceCheckRepository = jurisprudenceCheckRepository;
        this.anthropicService = anthropicService;
        this.webSearchService = webSearchService;
        this.maxWebSearchesPerCase = maxWebSearchesPerCase;
    }

    /**
     * Vérifie les références jurisprudentielles des documents du dossier de
     * {@code analysis} et persiste les {@link JurisprudenceCheck}.
     *
     * <p><strong>Fail-open</strong> : toute exception est journalisée sans
     * propagation — l'analyse du dossier reste {@code DONE}.</p>
     *
     * @param analysis analyse {@code DONE} dont on vérifie les documents
     */
    @Transactional
    public void verifyForAnalysis(CaseAnalysis analysis) {
        if (analysis == null || analysis.getId() == null) {
            return;
        }
        try {
            CaseFile caseFile = analysis.getCaseFile();
            if (caseFile == null) {
                return;
            }
            UUID caseFileId = caseFile.getId();

            List<Document> documents = documentRepository
                    .findByCaseFile_IdOrderByCreatedAtDesc(caseFileId);
            if (documents.isEmpty()) {
                log.debug("F-179: no document for caseFile {} — jurisprudence verification skipped",
                        caseFileId);
                return;
            }
            if (documents.size() > MAX_DOCUMENTS) {
                documents = documents.subList(0, MAX_DOCUMENTS);
            }

            Map<UUID, String> textByDocId = loadExtractedText(documents);
            if (textByDocId.isEmpty()) {
                log.debug("F-179: no extracted text for caseFile {} — verification skipped", caseFileId);
                return;
            }

            String userPrompt = buildUserPrompt(documents, textByDocId);
            if (userPrompt.isBlank()) {
                return;
            }

            // Un seul appel Sonnet : extraction (complément Claude) + vérification
            // fusionnées. System prompt mis en cache (F-142-04) pour réutilisation.
            AiCallContext ctx = AiCallContext.systemLevel(
                    JobType.SYSTEM_JURISPRUDENCE_VERIFICATION, caseFileId);
            AnthropicResult result = anthropicService.analyzeWithSystemCache(
                    ctx, SYSTEM_PROMPT, userPrompt, VERIFICATION_MAX_TOKENS);

            List<JurisprudenceCheck> checks = parseAndBuildChecks(
                    result.content(), analysis, caseFile, documents);
            if (checks.isEmpty()) {
                log.debug("F-179: no jurisprudence reference verified for caseFile {}", caseFileId);
                return;
            }
            jurisprudenceCheckRepository.saveAll(checks);
            log.info("F-179: {} jurisprudence check(s) persisted for caseFile {}",
                    checks.size(), caseFileId);

            // F-179 SF-179-02 — fallback web search Légifrance / Juridat sur les
            // checks incertains. Tout le bloc est fail-open : une exception laisse
            // les checks dans leur état issu de la vérification Sonnet.
            try {
                enrichWithWebSearch(checks, caseFile);
            } catch (Exception e) {
                log.warn("F-179: web search enrichment fail-open for caseFile {} — {}",
                        caseFileId, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("F-179: verifyForAnalysis fail-open for analysis {} — {}",
                    analysis.getId(), e.getMessage());
        }
    }

    // ---- fallback web search (SF-179-02) ----

    /**
     * Pour les checks incertains, tente une recherche web publique pour
     * confirmer l'existence de l'arrêt. Met à jour {@code statut} /
     * {@code sourceUrl} / {@code webSearchUsed} et re-persiste les checks
     * modifiés. Le web search statue UNIQUEMENT sur l'existence : un check
     * {@code SUSPECT} (position détournée) n'est jamais re-jugé.
     */
    private void enrichWithWebSearch(List<JurisprudenceCheck> checks, CaseFile caseFile) {
        if (webSearchService == null || checks.isEmpty()) {
            return;
        }
        String workspaceCountry = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        int budget = maxWebSearchesPerCase > 0
                ? maxWebSearchesPerCase : DEFAULT_MAX_WEB_SEARCHES_PER_CASE;
        int used = 0;
        List<JurisprudenceCheck> updated = new ArrayList<>();

        for (JurisprudenceCheck check : checks) {
            if (used >= budget) {
                break;
            }
            if (!shouldWebSearch(check)) {
                continue;
            }
            used++;
            String country = deduceCountry(check.getReference(), workspaceCountry);
            WebSearchResult res = webSearchService.searchJurisprudence(check.getReference(), country);
            check.setWebSearchUsed(true);
            switch (res.outcome()) {
                case FOUND -> {
                    // Existence confirmée — promotion en VERIFIED si le statut
                    // initial était incertain. La fidélité reste hors champ du
                    // web search : un SUSPECT n'arrive jamais ici (cf. shouldWebSearch).
                    check.setStatut(JurisprudenceCheckStatus.VERIFIED);
                    check.setSourceUrl(res.sourceUrl());
                }
                case NOT_FOUND -> check.setStatut(JurisprudenceCheckStatus.NOT_FOUND);
                case UNCERTAIN -> check.setStatut(JurisprudenceCheckStatus.UNCERTAIN);
            }
            updated.add(check);
        }
        if (!updated.isEmpty()) {
            jurisprudenceCheckRepository.saveAll(updated);
            log.info("F-179: web search enriched {} jurisprudence check(s) ({} searches used)",
                    updated.size(), used);
        }
    }

    /**
     * Un check est web-searché si son statut justifie une vérification
     * complémentaire : {@code UNCERTAIN} systématiquement, {@code NOT_FOUND}
     * uniquement si la confiance Claude n'est pas {@code HIGH} (un faux
     * {@code NOT_FOUND} discréditerait l'outil). {@code VERIFIED} et
     * {@code SUSPECT} ne sont jamais web-searchés.
     */
    static boolean shouldWebSearch(JurisprudenceCheck check) {
        JurisprudenceCheckStatus statut = check.getStatut();
        if (statut == JurisprudenceCheckStatus.UNCERTAIN) {
            return true;
        }
        if (statut == JurisprudenceCheckStatus.NOT_FOUND) {
            return !"HIGH".equalsIgnoreCase(check.getClaudeConfidence());
        }
        return false;
    }

    /**
     * Déduit le pays cible du web search à partir du format de la référence.
     * Les marqueurs belges l'emportent ; sinon repli sur le pays du workspace ;
     * à défaut {@code FRANCE}.
     */
    static String deduceCountry(String reference, String workspaceCountry) {
        if (reference != null) {
            String lower = reference.toLowerCase(java.util.Locale.ROOT);
            boolean belgianFormat = lower.contains("trib. trav.")
                    || lower.contains("tribunal du travail")
                    || lower.contains("cour const")
                    || lower.contains("c. const")
                    || lower.contains("grondwettelijk")
                    || lower.contains(" be ")
                    || lower.contains("belg");
            if (belgianFormat) {
                return "BELGIQUE";
            }
            // Formats clairement français.
            if (lower.contains("cass. soc") || lower.contains("conseil d")
                    || lower.startsWith("ce ") || lower.contains("cons. const")) {
                return "FRANCE";
            }
        }
        if (workspaceCountry != null && !workspaceCountry.isBlank()) {
            return workspaceCountry;
        }
        return "FRANCE";
    }

    // ---- chargement du texte ----

    private Map<UUID, String> loadExtractedText(List<Document> documents) {
        List<UUID> docIds = documents.stream().map(Document::getId).toList();
        Map<UUID, String> out = new HashMap<>();
        for (DocumentExtraction ex : documentExtractionRepository.findByDocumentIdIn(docIds)) {
            if (ex.getDocument() == null) {
                continue;
            }
            if (ex.getExtractionStatus() == ExtractionStatus.DONE
                    && ex.getExtractedText() != null
                    && !ex.getExtractedText().isBlank()) {
                out.put(ex.getDocument().getId(), ex.getExtractedText());
            }
        }
        return out;
    }

    // ---- construction du prompt ----

    private String buildUserPrompt(List<Document> documents, Map<UUID, String> textByDocId) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DOCUMENTS DU DOSSIER À VÉRIFIER ===\n\n");
        boolean any = false;
        for (Document doc : documents) {
            String text = textByDocId.get(doc.getId());
            if (text == null || text.isBlank()) {
                continue;
            }
            any = true;
            String filename = doc.getOriginalFilename() != null
                    ? doc.getOriginalFilename() : ("document-" + doc.getId());
            String slice = text.length() <= MAX_DOC_TEXT_CHARS
                    ? text : text.substring(0, MAX_DOC_TEXT_CHARS) + " [...]";
            List<String> candidates = JurisprudenceReferenceExtractor.extract(text);

            sb.append("--- Document : ").append(filename).append(" ---\n");
            sb.append("Références candidates pré-détectées (indices) : ");
            if (candidates.isEmpty()) {
                sb.append("(aucune)\n");
            } else {
                sb.append('\n');
                for (String c : candidates) {
                    sb.append("  - ").append(c).append('\n');
                }
            }
            sb.append("Extrait du texte :\n")
                    .append(slice.replace("\n", " ").trim())
                    .append("\n\n");
        }
        return any ? sb.toString() : "";
    }

    // ---- parsing de la réponse Sonnet ----

    private List<JurisprudenceCheck> parseAndBuildChecks(String rawContent,
                                                         CaseAnalysis analysis,
                                                         CaseFile caseFile,
                                                         List<Document> documents) {
        List<JurisprudenceCheck> out = new ArrayList<>();
        if (rawContent == null || rawContent.isBlank()) {
            return out;
        }
        java.util.Set<String> knownDocNames = new java.util.HashSet<>();
        for (Document doc : documents) {
            if (doc.getOriginalFilename() != null) {
                knownDocNames.add(doc.getOriginalFilename());
            }
        }
        try {
            String stripped = CaseAnalysisResponse.stripMarkdownCodeBlock(rawContent);
            JsonNode root = MAPPER.readTree(stripped);
            JsonNode checksNode = root.get("checks");
            if (checksNode == null || !checksNode.isArray()) {
                return out;
            }
            for (JsonNode node : checksNode) {
                String reference = textOrNull(node, "reference");
                if (reference == null) {
                    continue;
                }
                String documentName = textOrNull(node, "documentName");
                // Ancrage défensif : un nom de document inventé par le modèle est
                // ramené au premier document connu plutôt que d'être rejeté.
                if (documentName == null || !knownDocNames.contains(documentName)) {
                    documentName = knownDocNames.isEmpty()
                            ? "document inconnu"
                            : knownDocNames.iterator().next();
                }
                JurisprudenceCheck check = new JurisprudenceCheck();
                check.setCaseFile(caseFile);
                check.setCaseAnalysis(analysis);
                check.setWorkspace(caseFile.getWorkspace());
                check.setDocumentName(truncate(documentName, 500));
                check.setReference(truncate(reference, 500));
                check.setStatut(JurisprudenceCheckStatus.fromModelValue(textOrNull(node, "statut")));
                check.setExplication(truncate(textOrNull(node, "explication"), 2_000));
                check.setPositionAlleguee(truncate(textOrNull(node, "positionAlleguee"), 2_000));
                check.setClaudeConfidence(normalizeConfidence(textOrNull(node, "claudeConfidence")));
                check.setWebSearchUsed(false);
                out.add(check);
            }
        } catch (Exception e) {
            log.warn("F-179: failed to parse Sonnet verification JSON — {}", e.getMessage());
            return List.of();
        }
        return out;
    }

    private static String normalizeConfidence(String raw) {
        if (raw == null) {
            return null;
        }
        String up = raw.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (up) {
            case "HIGH", "MEDIUM", "LOW" -> up;
            default -> null;
        };
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        String s = child.asText();
        return (s == null || s.isBlank() || "null".equalsIgnoreCase(s)) ? null : s.trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
