package fr.ailegalcase.casefile;

/**
 * SF-FA-17-01 : requête d'analyse de recevabilité d'une procédure de partage
 * judiciaire (FR — art. 840 et s. Cciv + 1364 et s. CPC).
 *
 * <p>Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.</p>
 */
public record PartageJudiciaireRequest(
        PartageJudiciaireCalculator.EtapeProcedure etapeProcedure,
        PartageJudiciaireCalculator.TypeBienIndivision typeBienIndivision,
        Integer nombreCoindivisaires,
        Double valeurEstimeeBiensEur,
        Boolean pvDifficultesEtabli,
        Boolean tentativeAmiableEpuiseuee,
        Boolean desaccordMotive
) {}
