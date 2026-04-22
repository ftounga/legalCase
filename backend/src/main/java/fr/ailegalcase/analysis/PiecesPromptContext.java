package fr.ailegalcase.analysis;

import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentPiece;
import fr.ailegalcase.document.DocumentPieceRepository;
import fr.ailegalcase.document.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * F-146 SF-146-01 : construit la section "Pièces des documents" injectée dans
 * les prompts utilisateur de {@code CaseAnalysisService} et
 * {@code EnrichedAnalysisService} pour que l'IA puisse produire des
 * {@code sourceRef} précis.
 *
 * <p>Le bloc construit ressemble à :
 * <pre>
 * === PIÈCES IDENTIFIÉES DANS LES DOCUMENTS ===
 * Document : dossier_complet.pdf
 *   - CONTRAT « Contrat de travail Dupont » (p. 1-2)
 *   - ATTESTATION « Attestation collègue » (p. 3-3)
 *   - SMS « Échanges Fatima / Anne » (p. 4-5)
 *
 * Document : bulletins.pdf
 *   - BULLETIN_PAIE « Bulletin janvier 2024 » (p. 1-1)
 * ===
 * </pre>
 */
@Service
public class PiecesPromptContext {

    private final DocumentRepository documentRepository;
    private final DocumentPieceRepository pieceRepository;

    public PiecesPromptContext(DocumentRepository documentRepository,
                               DocumentPieceRepository pieceRepository) {
        this.documentRepository = documentRepository;
        this.pieceRepository = pieceRepository;
    }

    /**
     * Retourne le bloc formaté listant les pièces par document pour ce case file.
     * Retourne une chaîne vide si aucun document n'a de pièce détectée (dossiers
     * pré-F-145 notamment).
     */
    public String buildContextForCaseFile(UUID caseFileId) {
        if (caseFileId == null) return "";
        List<Document> docs = documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId);
        if (docs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        boolean hasAnyPiece = false;
        StringBuilder body = new StringBuilder();
        for (Document doc : docs) {
            List<DocumentPiece> pieces = pieceRepository
                    .findByDocument_IdOrderByOrderIndexAsc(doc.getId());
            if (pieces.isEmpty()) continue;
            hasAnyPiece = true;
            body.append("Document : ").append(doc.getOriginalFilename()).append("\n");
            for (DocumentPiece p : pieces) {
                body.append("  - ").append(p.getType().name());
                if (p.getLabel() != null && !p.getLabel().isBlank()) {
                    body.append(" « ").append(p.getLabel()).append(" »");
                }
                body.append(" (p. ").append(p.getPageStart());
                if (p.getPageEnd() != p.getPageStart()) {
                    body.append("-").append(p.getPageEnd());
                }
                body.append(")\n");
            }
            body.append("\n");
        }
        if (!hasAnyPiece) return "";

        sb.append("=== PIÈCES IDENTIFIÉES DANS LES DOCUMENTS ===\n");
        sb.append(body);
        sb.append("===\n");
        return sb.toString();
    }

    /** Pour tests : construit le contexte à partir d'une liste passée directement. */
    static String buildFromPieces(String documentName, List<DocumentPiece> pieces) {
        if (pieces == null || pieces.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("=== PIÈCES IDENTIFIÉES DANS LES DOCUMENTS ===\n");
        sb.append("Document : ").append(documentName).append("\n");
        for (DocumentPiece p : pieces) {
            sb.append("  - ").append(p.getType().name());
            if (p.getLabel() != null && !p.getLabel().isBlank()) {
                sb.append(" « ").append(p.getLabel()).append(" »");
            }
            sb.append(" (p. ").append(p.getPageStart());
            if (p.getPageEnd() != p.getPageStart()) {
                sb.append("-").append(p.getPageEnd());
            }
            sb.append(")\n");
        }
        sb.append("===\n");
        return sb.toString();
    }
}
