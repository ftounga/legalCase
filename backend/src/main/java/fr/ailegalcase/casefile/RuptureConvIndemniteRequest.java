package fr.ailegalcase.casefile;

import java.math.BigDecimal;

public record RuptureConvIndemniteRequest(
        Integer ancienneteAnnees,
        BigDecimal salaireMensuel
) {}
