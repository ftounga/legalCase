package fr.ailegalcase.casefile;

/**
 * SF-FA-18-09 : requête d'analyse de recevabilité d'une adoption
 * (FR — art. 343-370-2 Cciv).
 *
 * <p>Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.</p>
 */
public record AdoptionRequest(
        AdoptionCalculator.FormeAdoption formeAdoption,
        Integer ageAdoptant,
        Integer ageAdopte,
        Boolean consentementParents,
        Boolean consentementAdopte,
        Boolean consentementConjointAdoptant,
        Boolean enquetes,
        Boolean placement6mois,
        Boolean pupilleEtat,
        Boolean adoptantMarie
) {}
