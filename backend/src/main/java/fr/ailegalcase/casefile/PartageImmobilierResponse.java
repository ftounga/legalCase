package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.UUID;

public record PartageImmobilierResponse(
        UUID caseFileId,
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
