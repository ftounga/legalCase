package fr.ailegalcase.casefile;

import java.math.BigDecimal;

public record IndemniteComparatifRequest(
        String country,
        String typeRupture,
        int ancienneteAnnees,
        int ancienneteMois,
        int age,
        BigDecimal salaireMensuel
) {}
