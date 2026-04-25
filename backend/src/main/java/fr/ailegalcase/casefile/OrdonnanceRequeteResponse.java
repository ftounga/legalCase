package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-FA-23-01 : réponse de l'endpoint {@code /ordonnance-requete-analysis}.
 */
public record OrdonnanceRequeteResponse(
        UUID caseFileId,
        OrdonnanceRequeteCalculator.MotifRequete motifRequete,
        int scoreEligibilite,
        OrdonnanceRequeteCalculator.VerdictAccordeProbabilite verdictAccordeProbabilite,
        List<OrdonnanceRequeteCalculator.CritereRecevabilite> criteresRemplis,
        List<OrdonnanceRequeteCalculator.CritereRecevabilite> criteresManquants,
        int delaiTypiqueJoursMin,
        int delaiTypiqueJoursMax,
        int recoursAdverseDelaiJours,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
