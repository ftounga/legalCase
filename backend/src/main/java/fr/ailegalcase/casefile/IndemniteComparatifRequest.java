package fr.ailegalcase.casefile;

import java.math.BigDecimal;

public record IndemniteComparatifRequest(
        String country,
        int ancienneteAnnees,
        int age,
        BigDecimal salaireMensuel
) {}
