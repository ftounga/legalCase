package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-FA-17-01 : résultat structuré du calcul de recevabilité d'une procédure
 * de partage judiciaire (FR — art. 840 et s. Cciv + 1364 et s. CPC).
 */
public record PartageJudiciaireResult(
        PartageJudiciaireCalculator.EtapeProcedure etapeProcedure,
        PartageJudiciaireCalculator.TypeBienIndivision typeBienIndivision,
        int nombreCoindivisaires,
        double valeurEstimeeBiensEur,
        boolean pvDifficultesEtabli,
        boolean tentativeAmiableEpuiseuee,
        boolean desaccordMotive,
        String country,
        PartageJudiciaireCalculator.VerdictRecevabilite verdictRecevabilite,
        int scoreEligibilite,
        int dureeProcedureMois,
        double fraisEstimesEur,
        boolean risqueLicitation,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
