package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AncienneteResponse(
        UUID caseFileId,
        String conventionCode,
        java.time.LocalDate dateEntree,
        BigDecimal salaireBase,
        Integer congesContrat,
        BigDecimal primeContrat,
        String conventionLabel,
        String country,
        int ancienneteAnnees,
        int ancienneteMois,
        int congesLegauxJours,
        int congesSupplementairesJours,
        int congesTotalJours,
        BigDecimal primeAnciennetePourcentage,
        BigDecimal primeAncienneteMontant,
        List<EcartData> ecarts
) {
    public record EcartData(String champ, String attendu, String contractuel, String verdict) {}
}
