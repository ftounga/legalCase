package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-FA-24-01 : résultat structuré du calcul de dévolution légale successorale
 * (FR — art. 731 et s. Cciv).
 */
public record DevolutionLegaleResult(
        boolean conjointSurvivant,
        int nbDescendants,
        boolean tousDescendantsCommunsAvecConjoint,
        int nbDescendantsPredecedes,
        int nbPetitsEnfantsParRepresentation,
        boolean pereVivant,
        boolean mereVivant,
        int nbFreresSoeurs,
        int nbFreresSoeursPredecedes,
        boolean ascendantsOrdinaires,
        boolean collateralOrdinaires,
        DevolutionLegaleCalculator.OptionConjoint optionConjoint,
        String country,
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
        List<String> messages
) {}
