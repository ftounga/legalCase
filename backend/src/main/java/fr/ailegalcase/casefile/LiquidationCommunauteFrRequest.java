package fr.ailegalcase.casefile;

/**
 * SF-216-05 : body POST /api/v1/case-files/{id}/liquidation-communaute-fr.
 * <p>
 * Restauration / extension F-FA-04 (DELETE migration 191 + wrapper SF-198-03).
 * Outil single-country FRANCE — art. 1467-1517 Cciv + art. 1433-1435 Cciv
 * (récompenses) + art. 815-832 Cciv (indivision résiduelle).
 * </p>
 * <p>
 * Tous les montants en euros entiers (Integer). Le calculateur traite null
 * comme 0, mais le service rejette les valeurs négatives en 400.
 * </p>
 */
public record LiquidationCommunauteFrRequest(
        Integer valeurImmeubleCommun1Eur,
        Integer valeurImmeubleCommun2Eur,
        Integer capitalRestantDuEur,
        Integer valeurMobilierCommunEur,
        Integer autresActifsCommunsEur,
        Integer recompensesEpoux1Eur,
        Integer recompensesEpoux2Eur,
        Integer biensPropresEpoux1Eur,
        Integer biensPropresEpoux2Eur,
        OccupationLogementEnum occupationLogementEpoux,
        String regimeMatrimonialDetecte
) {}
