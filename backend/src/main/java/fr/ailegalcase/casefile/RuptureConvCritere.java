package fr.ailegalcase.casefile;

/**
 * Critère de validité d'une rupture conventionnelle individuelle (F-DT-10).
 */
public record RuptureConvCritere(
        String code,
        String label,
        String country,
        String description,
        int poids,
        boolean bloquant,
        String baseJuridique
) {}
