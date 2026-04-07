package fr.ailegalcase.casefile;

/**
 * Pièce à fournir pour un divorce par consentement mutuel.
 */
public record DivorcePiece(
        String code,
        String label,
        String country,
        String description,
        boolean obligatoire
) {}
