package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-216-05 : réponse API /api/v1/case-files/{id}/liquidation-communaute-fr.
 */
public record LiquidationCommunauteFrResponse(
        UUID caseFileId,
        int masseCommuneEur,
        int quotaPartEpoux1Eur,
        int quotaPartEpoux2Eur,
        int soulteEur,
        int recompensesNettesEpoux1Eur,
        int recompensesNettesEpoux2Eur,
        boolean alerteIndivision,
        String baseJuridique,
        List<String> messages,
        List<String> alertes,
        String country
) {}
