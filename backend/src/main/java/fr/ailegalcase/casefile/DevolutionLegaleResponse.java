package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-FA-24-01 : réponse de l'endpoint {@code /devolution-legale-analysis}.
 */
public record DevolutionLegaleResponse(
        UUID caseFileId,
        DevolutionLegaleCalculator.OrdreActif ordreActif,
        List<DevolutionLegaleCalculator.HeritierDesigne> heritiersDesignes,
        double quotePartConjoint,
        DevolutionLegaleCalculator.ModaliteHeritier modaliteConjoint,
        boolean representationActive,
        boolean fenteApplicable,
        int scoreEligibilite,
        List<String> risquesContentieux,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
