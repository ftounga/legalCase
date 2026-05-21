package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-05 : résultat du calcul de liquidation de la communauté légale FR
 * (art. 1467-1517 Cciv + art. 1433-1435 Cciv).
 */
public record LiquidationCommunauteFrResult(
        int masseCommuneEur,
        int quotaPartEpoux1Eur,
        int quotaPartEpoux2Eur,
        int soulteEur,
        int recompensesNettesEpoux1Eur,
        int recompensesNettesEpoux2Eur,
        boolean alerteIndivision,
        String baseJuridique,
        List<String> messages,
        List<String> alertes
) {}
