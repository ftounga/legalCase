package fr.ailegalcase.casefile;

/**
 * SF-212-03 : données d'entrée du calculateur de validité du forfait jours
 * (FRANCE — L. 3121-58 à L. 3121-66 CT ; Cass. soc. 29/06/2011 n°09-71.107).
 */
public record ForfaitJoursValiditeInput(
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
) {}
