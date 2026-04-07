package fr.ailegalcase.casefile;

import java.math.BigDecimal;

public record PartageImmobilierRequest(
        String country,
        BigDecimal valeurVenale,
        BigDecimal capitalRestantDu,
        BigDecimal quotePartAttributaire,
        boolean isDivorce
) {}
