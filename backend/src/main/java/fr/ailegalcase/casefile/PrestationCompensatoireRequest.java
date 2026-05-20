package fr.ailegalcase.casefile;

/**
 * SF-216-01 : body POST /api/v1/case-files/{id}/prestation-compensatoire.
 * <p>
 * Restauration F-FA-01 (DELETE migration 191). Outil single-country FRANCE.
 * Critères art. 272 Cciv (durée, revenus, patrimoine, âge).
 * </p>
 */
public record PrestationCompensatoireRequest(
        Integer dureeMariageAnnees,
        Integer revenusAnnuelsEpoux1Eur,
        Integer revenusAnnuelsEpoux2Eur,
        Integer patrimoinePropre1Eur,
        Integer patrimoinePropre2Eur,
        Integer ageEpoux1Annees,
        Integer ageEpoux2Annees,
        FormePrestationEnum formePrestationDemandee,
        Boolean avantageMatrimonialDetecte
) {}
