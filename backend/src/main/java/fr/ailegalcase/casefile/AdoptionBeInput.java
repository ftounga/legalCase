package fr.ailegalcase.casefile;

/**
 * SF-223-02 : input du moteur décisionnel BE de recevabilité de l'adoption.
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.
 * {@code ecartAgeAnnees} est nullable (dérivé de la différence des âges s'il
 * n'est pas saisi). {@code lienCohabitationLegaleOuMariage} et
 * {@code adoptantEnCoupleAvecParentBiologique} sont pertinents surtout pour la
 * branche CO_PARENTALE.</p>
 */
public record AdoptionBeInput(
        AdoptionBeCalculator.TypeAdoptionBe typeAdoption,
        Integer ageAdoptant,
        Integer ageAdopte,
        Integer ecartAgeAnnees,
        Boolean adoptantEnCoupleAvecParentBiologique,
        Boolean lienCohabitationLegaleOuMariage,
        Boolean agrementObtenu,
        Boolean consentementAdopteOuRepresentants,
        AdoptionBeCalculator.ConsentementParentsBiologiques consentementParentsBiologiques,
        String commentaire
) {}
