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
            (ex: contrat + CNI + SMS + attestation). Tu dois détecter les ruptures
            logiques et lister chaque pièce avec son type parmi cette liste exacte :
            - CONTRAT : contrat de travail, avenant, convention
            - PIECE_IDENTITE : CNI, passeport, titre de séjour
            - SMS : échanges SMS/messagerie
            - EMAIL : emails imprimés
            - ATTESTATION : attestation manuscrite, témoignage
            - BULLETIN_PAIE : bulletins de paie
            - LETTRE : courrier formel (licenciement, mise en demeure, convocation…)
            - PHOTO : photos non-textuelles
            - AUTRE : tout le reste

            Pour chaque pièce, fournir :
            - type : enum ci-dessus
            - label : description courte contextuelle (ex: "Contrat de travail Dupont",
              "Attestation collègue 1", "SMS échangés du 12/01/2024")
            - pageStart, pageEnd : pages dans le document (1-indexed ; si inconnu, estime)
            - orderIndex : ordre séquentiel dans le document (0-indexed)

            Si le document est unitaire (une seule pièce), retourner 1 entrée.

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
            AnthropicResult result = anthropicService.analyzeFast(SYSTEM_PROMPT, truncated, 2048);
            parsed = parseResponse(result.content());
            if (parsed.isEmpty()) {
                log.info("Haiku returned empty piece list for document {} — fallback to AUTRE", documentId);
                parsed = fallback();
            }
        } catch (Exception e) {
            log.warn("Piece detection failed for document {} — fallback to AUTRE ({})",
                    documentId, e.getMessage());
            parsed = fallback();
        }

        persistAll(extraction.getDocument(), parsed);
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
