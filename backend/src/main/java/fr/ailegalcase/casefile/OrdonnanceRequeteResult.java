package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-FA-23-01 : résultat structuré du calcul de probabilité d'obtention d'une
 * ordonnance sur requête (mesures urgentes familiales — art. 493 CPC FR / 1025 CJ BE).
 */
public record OrdonnanceRequeteResult(
        OrdonnanceRequeteCalculator.MotifRequete motifRequete,
        boolean urgenceJustifiee,
        boolean derogationContradictoireJustifiee,
        boolean pieceJustificativeFournie,
        boolean presenceEnfants,
        String country,
        int scoreEligibilite,
        OrdonnanceRequeteCalculator.VerdictAccordeProbabilite verdictAccordeProbabilite,
        List<OrdonnanceRequeteCalculator.CritereRecevabilite> criteresRemplis,
        List<OrdonnanceRequeteCalculator.CritereRecevabilite> criteresManquants,
        int delaiTypiqueJoursMin,
        int delaiTypiqueJoursMax,
        int recoursAdverseDelaiJours,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
