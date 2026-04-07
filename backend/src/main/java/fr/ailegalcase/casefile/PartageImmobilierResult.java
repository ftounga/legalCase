package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * Résultat du simulateur de partage immobilier.
 */
public record PartageImmobilierResult(
        String country,
        BigDecimal valeurVenale,
        BigDecimal capitalRestantDu,
        BigDecimal valeurNette,
        BigDecimal quotePartAttributaire,
        BigDecimal quotePartCedant,
        BigDecimal partAttributaire,
        BigDecimal partCedant,
        BigDecimal soulte,
        BigDecimal droitPartage,
        BigDecimal tauxDroitPartage,
        BigDecimal fraisNotaireEstimes,
        BigDecimal coutTotal,
        String baseJuridique,
        String commentaire
) {}
