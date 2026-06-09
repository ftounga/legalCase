package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.document.DocumentPieceType;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * F-98 / SF-98-57 — assemblage <strong>déterministe (hors-LLM)</strong> de l'annexe
 * « Bordereau de pièces communiquées » qui clôt le projet de conclusions.
 *
 * <p>La section est construite directement à partir de la liste des pièces numérotées
 * déjà chargée pour le prompt ({@link CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece},
 * source {@code piece_number} persistant — F-260). Aucune instruction n'est laissée au
 * modèle : la correspondance avec les renvois « Pièce n° X » du corps est garantie par
 * construction et aucune pièce ne peut être hallucinée.</p>
 *
 * <p><strong>Anti-jargon (non-régression SF-98-55)</strong> : jamais de nom de fichier
 * brut ni de token d'enum (ex. {@code LETTRE}). Le label métier est l'intitulé principal ;
 * le type n'est ajouté que traduit en libellé français lisible et seulement s'il enrichit
 * (label vide ou ne contenant pas déjà le libellé du type). À défaut de label, repli sur
 * un libellé neutre lisible.</p>
 */
final class BordereauPiecesBuilder {

    static final String HEADER = "\n\n## BORDEREAU DE PIÈCES COMMUNIQUÉES\n\n";

    /** Libellé neutre lisible quand une pièce n'a ni label ni type exploitable. */
    private static final String NEUTRAL_LABEL = "Pièce communiquée";

    private BordereauPiecesBuilder() {
    }

    /**
     * Construit la section markdown du bordereau, triée par {@code number} croissant.
     *
     * @param pieces pièces numérotées du dossier (peut être {@code null} ou vide)
     * @return la section markdown, ou une <strong>chaîne vide</strong> si aucune pièce
     */
    static String buildBordereau(
            List<CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece> pieces) {
        if (pieces == null || pieces.isEmpty()) {
            return "";
        }
        String lines = pieces.stream()
                .sorted(java.util.Comparator.comparingInt(
                        CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece::number))
                .map(BordereauPiecesBuilder::formatLine)
                .collect(Collectors.joining("\n"));
        return HEADER + lines;
    }

    private static String formatLine(
            CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece piece) {
        String label = piece.label() != null ? piece.label().trim() : "";
        String typeLabel = humanType(piece.type());

        String intitule;
        if (!label.isEmpty()) {
            intitule = label;
            // N'ajouter le type que s'il enrichit et n'est pas déjà contenu dans le label.
            if (typeLabel != null
                    && !label.toLowerCase(Locale.FRENCH).contains(typeLabel.toLowerCase(Locale.FRENCH))) {
                intitule = label + " (" + typeLabel + ")";
            }
        } else if (typeLabel != null) {
            intitule = typeLabel;
        } else {
            intitule = NEUTRAL_LABEL;
        }
        return piece.number() + ". " + intitule;
    }

    /**
     * Traduit le {@code type} brut (nom d'enum {@link DocumentPieceType}) en libellé
     * français lisible. Retourne {@code null} si le type est absent ou non informatif
     * (un {@code AUTRE} « Pièce » n'enrichit pas un label déjà présent).
     */
    private static String humanType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }
        DocumentPieceType type = DocumentPieceType.fromStringOrDefault(rawType);
        if (type == DocumentPieceType.AUTRE) {
            return null;
        }
        return type.frenchLabel();
    }
}
