package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * Résultat de la génération d'un recours structuré.
 */
public record GeneratedRecours(
        String recoursType,
        String enTete,
        String objetDemande,
        String visaTextes,
        String exposeFaits,
        String moyensDroit,
        String conclusions,
        List<String> piecesJointes,
        LocalDate dateLimite,
        boolean dateLimiteDepassee,
        String avertissement
) {}
