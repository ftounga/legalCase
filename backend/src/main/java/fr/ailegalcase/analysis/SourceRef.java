package fr.ailegalcase.analysis;

/**
 * F-146 SF-146-01 : référence précise à une pièce juridique dans un document.
 *
 * <p>Produit par le pipeline IA (CaseAnalysis, DocumentAnalysis, EnrichedAnalysis)
 * pour chaque citation de source. Permet à l'avocat de remonter à la preuve
 * exacte — pas seulement "dossier_complet.pdf" mais
 * "dossier_complet.pdf · Contrat de travail Dupont · p. 1-2".
 *
 * <p>Tous les champs peuvent être null si l'IA n'a pas fourni l'info (ex:
 * item legacy pré-F-146, ou citation sans sourceRef). Dans ce cas le frontend
 * affiche le fallback {@code source} simple.
 */
public record SourceRef(
        String documentName,
        String pieceType,
        String pieceLabel,
        Integer pageStart,
        Integer pageEnd
) {
    /** Fabrique un ref "minimal" (juste le nom de document). */
    public static SourceRef ofDocumentOnly(String documentName) {
        return new SourceRef(documentName, null, null, null, null);
    }
}
