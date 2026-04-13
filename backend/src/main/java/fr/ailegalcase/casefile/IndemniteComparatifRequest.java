package fr.ailegalcase.casefile;

import java.math.BigDecimal;

public record IndemniteComparatifRequest(
        String country,
        String typeRupture,
        int ancienneteAnnees,
        int age,
        BigDecimal salaireMensuel
) {}
