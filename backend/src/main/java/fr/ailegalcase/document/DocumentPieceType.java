package fr.ailegalcase.document;

/**
 * SF-145-01 : types contraints de pièces identifiables dans un document composite.
 * Liste fermée — toute valeur non mappée remonte en AUTRE (fail-open côté parseur).
 */
public enum DocumentPieceType {
    CONTRAT,
    PIECE_IDENTITE,
    SMS,
    EMAIL,
    ATTESTATION,
    BULLETIN_PAIE,
    LETTRE,
    PHOTO,
    AUTRE;

    public static DocumentPieceType fromStringOrDefault(String raw) {
        if (raw == null) return AUTRE;
        try {
            return DocumentPieceType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AUTRE;
        }
    }
}
