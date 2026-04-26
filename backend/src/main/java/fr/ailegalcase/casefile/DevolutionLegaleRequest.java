package fr.ailegalcase.casefile;

/**
 * SF-FA-24-01 : requête d'analyse de dévolution légale successorale (FR — art.
 * 731 et s. Cciv). Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.
 */
public record DevolutionLegaleRequest(
        Boolean conjointSurvivant,
        Integer nbDescendants,
        Boolean tousDescendantsCommunsAvecConjoint,
        Integer nbDescendantsPredecedes,
        Integer nbPetitsEnfantsParRepresentation,
        Boolean pereVivant,
        Boolean mereVivant,
        Integer nbFreresSoeurs,
        Integer nbFreresSoeursPredecedes,
        Boolean ascendantsOrdinaires,
        Boolean collateralOrdinaires,
        DevolutionLegaleCalculator.OptionConjoint optionConjoint
) {}
