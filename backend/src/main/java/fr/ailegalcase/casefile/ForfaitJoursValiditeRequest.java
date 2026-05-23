package fr.ailegalcase.casefile;

/**
 * SF-212-03 : requête HTTP pour l'endpoint d'analyse de validité du forfait
 * jours (FRANCE — L. 3121-58 à L. 3121-66 CT).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record ForfaitJoursValiditeRequest(
        Boolean accordCollectifExiste,
        Boolean accordGarantitSuiviCharge,
        Boolean entretienAnnuelRealise,
        Boolean documentControleMensuelExiste,
        Boolean categorieAutonomeConfirmee,
        int nbJoursForfait,
        int ancienneteMois,
        double salaireMensuelBrutEuros,
        int hsEstimeesParSemaine,
        int nbSemainesParAn
) {

    ForfaitJoursValiditeInput toInput() {
        return new ForfaitJoursValiditeInput(
                accordCollectifExiste,
                accordGarantitSuiviCharge,
                entretienAnnuelRealise,
                documentControleMensuelExiste,
                categorieAutonomeConfirmee,
                nbJoursForfait,
                ancienneteMois,
                salaireMensuelBrutEuros,
                hsEstimeesParSemaine,
                nbSemainesParAn
        );
    }
}
