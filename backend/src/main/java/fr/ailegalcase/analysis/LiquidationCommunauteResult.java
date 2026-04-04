package fr.ailegalcase.analysis;

import java.util.List;

/**
 * Résultat structuré de la liquidation de communauté, extrait par l'IA depuis les documents.
 * Pas de calcul — inventaire des biens détectés.
 */
public record LiquidationCommunauteResult(
        String regimeMatrimonial,
        List<BienItem> actifCommun,
        List<BienItem> biensPropresEpouxA,
        List<BienItem> biensPropresEpouxB,
        List<BienItem> passifCommun
) {
    public record BienItem(String libelle, Double valeur) {}
}
