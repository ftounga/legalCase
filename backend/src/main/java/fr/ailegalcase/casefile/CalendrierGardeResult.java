package fr.ailegalcase.casefile;

import java.util.List;

/**
 * Résultat de la génération d'un calendrier de garde.
 */
public record CalendrierGardeResult(
        String gardeCode,
        String gardeLabel,
        String country,
        String parentANom,
        String parentBNom,
        String repartitionType,
        List<String> semaineTypeParentA,
        List<String> semaineTypeParentB,
        String vacancesRegle,
        int joursParAnParentA,
        int joursParAnParentB,
        String baseJuridique,
        String commentaire
) {}
