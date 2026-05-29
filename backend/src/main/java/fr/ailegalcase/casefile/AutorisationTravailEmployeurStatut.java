package fr.ailegalcase.casefile;

/**
 * SF-214-43 : statut de l'analyse des obligations de l'employeur recrutant un
 * travailleur étranger (outil F-IM-46-autorisation-travail-employeur-fr).
 *
 * <ul>
 *   <li>AUTORISATION_NON_REQUISE : candidat ressortissant UE/EEE/Suisse — libre
 *       circulation, aucune autorisation de travail préalable (L. 5221-2 1°).</li>
 *   <li>AUTORISATION_REQUISE : candidat ressortissant d'un pays tiers — autorisation
 *       de travail préalable obligatoire (L. 5221-1 Code du travail).</li>
 *   <li>RECOURS_POSSIBLE : refus d'autorisation notifié, délai de recours devant le
 *       tribunal administratif (2 mois) encore ouvert.</li>
 *   <li>RECOURS_PRESCRIT : refus d'autorisation notifié, délai de recours dépassé.</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (droit des étrangers français — côté employeur).
 */
public enum AutorisationTravailEmployeurStatut {
    AUTORISATION_NON_REQUISE,
    AUTORISATION_REQUISE,
    RECOURS_POSSIBLE,
    RECOURS_PRESCRIT
}
