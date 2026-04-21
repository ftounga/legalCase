package fr.ailegalcase.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * SF-145-01 : détection automatique des pièces juridiques distinctes à
 * l'intérieur d'un document uploadé. Écoute {@link ExtractionDoneEvent}
 * en parallèle de {@link ChunkingService}. Fail-open : toute erreur Haiku ou
 * JSON invalide est fallback sur une pièce unique {@code AUTRE}.
 */
@Service
public class DocumentPieceDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentPieceDetectionService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Budget tokens input Haiku — texte tronqué à ~50k chars (≈ 12k tokens). */
    static final int MAX_TEXT_CHARS_FOR_PROMPT = 50_000;

    static final String SYSTEM_PROMPT = """
            Tu identifies les pièces juridiques distinctes présentes dans un document
            composite. Un document peut contenir plusieurs pièces scannées en une passe
            (ex: contrat de 5 pages + CNI recto-verso + échanges SMS imprimés).

            RÈGLE CENTRALE : **regroupe les pages qui appartiennent à la même pièce**.
            Une pièce unique peut s'étendre sur plusieurs pages — dans ce cas tu produis
            UNE SEULE entrée avec pageStart et pageEnd couvrant toutes les pages concernées.
            NE fragmente PAS un même document en autant de pièces qu'il a de pages.

            Indices de continuité entre pages consécutives (= même pièce) :
            - même en-tête, pied de page, numérotation « page X sur Y »
            - même mise en page, police, structure
            - phrase coupée par une rupture de page et qui reprend sur la suivante
            - signature, paraphe, mention « suite » ou « à suivre »
            - date identique, parties identiques, objet identique

            Rupture détectable (= nouvelle pièce) :
            - changement net de type de document (contrat → CNI, CNI → SMS)
            - nouvelle en-tête officielle, nouveau logo, nouvelle mise en page
            - nouvel émetteur/destinataire
            - changement radical de contenu sémantique

            Tailles typiques par type :
            - CONTRAT : souvent 2 à 10 pages (regroupe-les)
            - LETTRE formelle : 1 à 3 pages (licenciement, mise en demeure…)
            - ATTESTATION : 1 à 2 pages
            - BULLETIN_PAIE : 1 à 2 pages chacun (plusieurs bulletins = plusieurs pièces)
            - EMAIL : 1 page par email généralement
            - SMS : 1 à 2 pages
            - PIECE_IDENTITE : 1 page (recto OU recto-verso sur une même page)
            - PHOTO : 1 page

            En cas de doute sur une rupture, **préfère regrouper** plutôt que fragmenter.

            Types autorisés (liste exacte) :
            CONTRAT, PIECE_IDENTITE, SMS, EMAIL, ATTESTATION, BULLETIN_PAIE, LETTRE, PHOTO, AUTRE

            Pour chaque pièce, fournir :
            - type : enum ci-dessus
            - label : description courte contextuelle (ex: "Contrat de travail Dupont",
              "Attestation collègue 1", "SMS échangés du 12/01/2024")
            - pageStart, pageEnd : pages dans le document (1-indexed ; si inconnu, estime)
            - orderIndex : ordre séquentiel dans le document (0-indexed)

            EXEMPLE 1 — document composite typique :
            Entrée : PDF 9 pages contenant un contrat de travail (p. 1-5), une CNI
            recto-verso sur une page (p. 6), puis 3 captures SMS sur 3 pages (p. 7-9).
            Sortie attendue :
            [
              {"type":"CONTRAT","label":"Contrat de travail","pageStart":1,"pageEnd":5,"orderIndex":0},
              {"type":"PIECE_IDENTITE","label":"CNI","pageStart":6,"pageEnd":6,"orderIndex":1},
              {"type":"SMS","label":"Échanges SMS","pageStart":7,"pageEnd":9,"orderIndex":2}
            ]

            EXEMPLE 2 — document unitaire multi-pages :
            Entrée : PDF 7 pages, uniquement un contrat de travail avec clauses étendues
            et numérotation « page X sur 7 ».
            Sortie attendue :
            [
              {"type":"CONTRAT","label":"Contrat de travail","pageStart":1,"pageEnd":7,"orderIndex":0}
            ]
            **Attention** : ne produit surtout PAS 7 entrées séparées — c'est UNE pièce.

            Si le document est unitaire (une seule pièce), retourner 1 entrée couvrant
            toutes les pages.

            Réponds UNIQUEMENT avec un tableau JSON valide, sans texte avant ni après.
            Format : [{"type":"...","label":"...","pageStart":N,"pageEnd":N,"orderIndex":N}]
            """;

    private final DocumentExtractionRepository extractionRepository;
    private final DocumentPieceRepository pieceRepository;
    private final AnthropicService anthropicService;
    private final TaskExecutor taskExecutor;

    @Lazy @Autowired
    private DocumentPieceDetectionService self;

    public DocumentPieceDetectionService(DocumentExtractionRepository extractionRepository,
                                         DocumentPieceRepository pieceRepository,
                                         AnthropicService anthropicService,
                                         @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.extractionRepository = extractionRepository;
        this.pieceRepository = pieceRepository;
        this.anthropicService = anthropicService;
        this.taskExecutor = taskExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExtractionDone(ExtractionDoneEvent event) {
        taskExecutor.execute(() -> self.detect(event.extractionId(), event.extractedText()));
    }

    @Transactional
    public void detect(UUID extractionId, String extractedText) {
        if (extractedText == null || extractedText.isBlank()) {
            log.warn("Extraction {} has empty text — skipping piece detection", extractionId);
            return;
        }

        DocumentExtraction extraction = extractionRepository.findById(extractionId).orElse(null);
        if (extraction == null || extraction.getDocument() == null) {
            log.warn("Extraction {} or its document not found — skipping piece detection", extractionId);
            return;
        }

        UUID documentId = extraction.getDocument().getId();

        // Idempotence : delete before insert (SF-145-01 critère U-04).
        pieceRepository.deleteByDocumentId(documentId);

        String truncated = extractedText.length() > MAX_TEXT_CHARS_FOR_PROMPT
                ? extractedText.substring(0, MAX_TEXT_CHARS_FOR_PROMPT)
                : extractedText;

        List<ParsedPiece> parsed;
        try {
            // SF-145-03 : Sonnet au lieu de Haiku — la détection de structure
            // documentaire demande plus de raisonnement que Haiku n'en offre.
            // Surcoût : ~0,002 € → ~0,01 €/doc. Qualité nettement supérieure.
            AnthropicResult result = anthropicService.analyze(SYSTEM_PROMPT, truncated, 2048);
            // SF-145-05 : log diagnostic de la réponse brute (tronquée 400 chars)
            // pour identifier les cas où Sonnet sur-segmente ou mal classifie.
            String rawContent = result.content();
            String preview = rawContent == null ? "<null>"
                    : rawContent.length() > 400 ? rawContent.substring(0, 400) + "…[truncated]" : rawContent;
            log.info("Sonnet response for document {} (input {} chars) :\n{}",
                    documentId, truncated.length(), preview);
            parsed = parseResponse(rawContent);
            if (parsed.isEmpty()) {
                log.info("Sonnet returned empty piece list for document {} — fallback to AUTRE", documentId);
                parsed = fallback();
            }
        } catch (Exception e) {
            log.warn("Piece detection failed for document {} — fallback to AUTRE ({})",
                    documentId, e.getMessage());
            parsed = fallback();
        }

        persistAll(extraction.getDocument(), parsed);
        // SF-145-05 : log détaillé des pièces persistées pour diagnostic
        for (ParsedPiece p : parsed) {
            log.info("  piece[{}] type={} pages={}-{} label=\"{}\"",
                    p.orderIndex, p.type, p.pageStart, p.pageEnd, p.label);
        }
        log.info("Piece detection done for document {} — {} piece(s) persisted", documentId, parsed.size());
    }

    private void persistAll(Document document, List<ParsedPiece> parsed) {
        for (ParsedPiece p : parsed) {
            DocumentPiece entity = new DocumentPiece();
            entity.setDocument(document);
            entity.setType(p.type);
            entity.setLabel(p.label);
            entity.setPageStart(p.pageStart);
            entity.setPageEnd(p.pageEnd);
            entity.setOrderIndex(p.orderIndex);
            pieceRepository.save(entity);
        }
    }

    private static List<ParsedPiece> fallback() {
        return List.of(new ParsedPiece(
                DocumentPieceType.AUTRE,
                "Document complet",
                1,
                1,
                0
        ));
    }

    /**
     * Parseur robuste : accepte un tableau JSON brut ou entouré de texte
     * (fallback sur la première accolade ouvrante d'un bloc array).
     */
    static List<ParsedPiece> parseResponse(String raw) throws Exception {
        if (raw == null || raw.isBlank()) return List.of();
        String cleaned = raw.trim();
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("No JSON array found in Haiku response");
        }
        String json = cleaned.substring(start, end + 1);
        JsonNode arr = MAPPER.readTree(json);
        if (!arr.isArray()) throw new IllegalArgumentException("Haiku response is not an array");

        List<ParsedPiece> out = new ArrayList<>();
        int i = 0;
        for (JsonNode node : arr) {
            String typeRaw = node.hasNonNull("type") ? node.get("type").asText() : "AUTRE";
            String label = node.hasNonNull("label") ? node.get("label").asText() : null;
            int pageStart = node.hasNonNull("pageStart") ? node.get("pageStart").asInt(1) : 1;
            int pageEnd = node.hasNonNull("pageEnd") ? node.get("pageEnd").asInt(pageStart) : pageStart;
            int orderIndex = node.hasNonNull("orderIndex") ? node.get("orderIndex").asInt(i) : i;
            out.add(new ParsedPiece(
                    DocumentPieceType.fromStringOrDefault(typeRaw),
                    label,
                    Math.max(1, pageStart),
                    Math.max(pageStart, pageEnd),
                    orderIndex
            ));
            i++;
        }
        return out;
    }

    record ParsedPiece(DocumentPieceType type, String label, int pageStart, int pageEnd, int orderIndex) {
        ParsedPiece {
            Objects.requireNonNull(type, "type must not be null");
        }
    }
}
