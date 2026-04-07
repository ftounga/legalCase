package fr.ailegalcase.casefile;

/**
 * Étape du processus de divorce par consentement mutuel.
 */
public record DivorceEtape(
        String code,
        String label,
        String country,
        int ordre,
        String description,
        String delai,
        boolean obligatoire
) {}
