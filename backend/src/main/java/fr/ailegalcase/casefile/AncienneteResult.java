package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Résultat du calcul d'ancienneté, congés et primes.
 */
public record AncienneteResult(
        String conventionCode,
        String conventionLabel,
        String country,
        int ancienneteAnnees,
        int ancienneteMois,
        int congesLegauxJours,
        int congesSupplementairesJours,
        int congesTotalJours,
        BigDecimal primeAnciennetePourcentage,
        BigDecimal primeAncienneteMontant,
        BigDecimal baremePrimePourcentage,
        int baremeCongesTotal,
        List<Ecart> ecarts
) {
    public record Ecart(String champ, String attendu, String contractuel, String verdict) {
        public static final String CONFORME = "CONFORME";
        public static final String ECART = "ECART";
    }
}
