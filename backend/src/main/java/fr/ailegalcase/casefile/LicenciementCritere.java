package fr.ailegalcase.casefile;

/**
 * Critère de validité d'un licenciement.
 */
public record LicenciementCritere(
        String code,
        String label,
        String country,
        String description,
        int poids,
        boolean bloquant,
        String baseJuridique
) {}
