package fr.ailegalcase.casefile;

/**
 * SF-223-02 : requête HTTP pour les endpoints de recevabilité de l'adoption BE.
 *
 * <p>Le pays cible est dérivé du workspace côté service — pas transmis dans le
 * body. {@code typeAdoption} et {@code consentementParentsBiologiques} sont
 * requis (validation portée par le Calculator → 400 via
 * {@code IllegalArgumentException}).</p>
 */
public record AdoptionBeRequest(
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
) {

    AdoptionBeInput toInput() {
        return new AdoptionBeInput(
                typeAdoption,
                ageAdoptant,
                ageAdopte,
                ecartAgeAnnees,
                adoptantEnCoupleAvecParentBiologique,
                lienCohabitationLegaleOuMariage,
                agrementObtenu,
                consentementAdopteOuRepresentants,
                consentementParentsBiologiques,
                commentaire);
    }
}
