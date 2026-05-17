package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-217-02 : résultat structuré du suivi de la procédure de liquidation-partage
 * belge (notaire commis).
 */
public record LiquidationPartageBeResult(
        LiquidationPartageBeCalculator.LiquidationPartageBeVerdict verdict,
        List<LiquidationPartageBeCalculator.EtapeProcedure> etapes,
        List<LiquidationPartageBeCalculator.DelaiProcedure> delais,
        String prochaineEtape,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
