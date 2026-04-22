package fr.ailegalcase.analysis;

import fr.ailegalcase.document.DocumentPiece;
import fr.ailegalcase.document.DocumentPieceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-146 SF-146-01 : tests du helper statique {@code buildFromPieces} de
 * {@link PiecesPromptContext}. La version instance {@code buildContextForCaseFile}
 * nécessite les repositories — testée indirectement via les IT.
 */
class PiecesPromptContextTest {

    // U-01 : plusieurs pièces → bloc formaté avec entête, doc name et liste
    @Test
    void buildFromPieces_multiplePieces_returnsFormattedBlock() {
        List<DocumentPiece> pieces = List.of(
                piece(DocumentPieceType.CONTRAT, "Contrat Dupont", 1, 2),
                piece(DocumentPieceType.SMS, "Échanges Anne", 3, 3),
                piece(DocumentPieceType.BULLETIN_PAIE, "Bulletin janv", 4, 4));

        String out = PiecesPromptContext.buildFromPieces("dossier.pdf", pieces);

        assertThat(out).startsWith("=== PIÈCES IDENTIFIÉES DANS LES DOCUMENTS ===");
        assertThat(out).contains("Document : dossier.pdf");
        assertThat(out).contains("- CONTRAT « Contrat Dupont » (p. 1-2)");
        assertThat(out).contains("- SMS « Échanges Anne » (p. 3)");
        assertThat(out).contains("- BULLETIN_PAIE « Bulletin janv » (p. 4)");
        assertThat(out).endsWith("===\n");
    }

    // U-02 : pièce sans label → bloc sans guillemets "«»"
    @Test
    void buildFromPieces_pieceWithoutLabel_omitsGuillemets() {
        List<DocumentPiece> pieces = List.of(piece(DocumentPieceType.PHOTO, null, 1, 1));

        String out = PiecesPromptContext.buildFromPieces("photo.jpg", pieces);

        assertThat(out).contains("- PHOTO (p. 1)");
        assertThat(out).doesNotContain("«");
    }

    // U-03 : aucune pièce → retour vide
    @Test
    void buildFromPieces_empty_returnsEmpty() {
        assertThat(PiecesPromptContext.buildFromPieces("doc.pdf", List.of())).isEmpty();
        assertThat(PiecesPromptContext.buildFromPieces("doc.pdf", null)).isEmpty();
    }

    // U-04 : page unique → notation "p. N" sans tiret
    @Test
    void buildFromPieces_singlePage_usesSinglePageNotation() {
        List<DocumentPiece> pieces = List.of(piece(DocumentPieceType.PIECE_IDENTITE, "CNI", 5, 5));
        String out = PiecesPromptContext.buildFromPieces("doc.pdf", pieces);
        assertThat(out).contains("(p. 5)");
        assertThat(out).doesNotContain("5-5");
    }

    // U-05 : page range → notation "p. N-M"
    @Test
    void buildFromPieces_pageRange_usesRangeNotation() {
        List<DocumentPiece> pieces = List.of(piece(DocumentPieceType.CONTRAT, "Contrat", 2, 7));
        String out = PiecesPromptContext.buildFromPieces("doc.pdf", pieces);
        assertThat(out).contains("(p. 2-7)");
    }

    private static DocumentPiece piece(DocumentPieceType type, String label, int start, int end) {
        DocumentPiece p = new DocumentPiece();
        p.setType(type);
        p.setLabel(label);
        p.setPageStart(start);
        p.setPageEnd(end);
        return p;
    }
}
