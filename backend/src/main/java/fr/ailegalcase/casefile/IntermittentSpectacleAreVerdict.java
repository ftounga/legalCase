package fr.ailegalcase.casefile;

/**
 * SF-218-17 : verdict global de l'analyse d'ouverture des droits à l'ARE pour
 * l'intermittent du spectacle (annexes 8 et 10 Unedic — seuil 507 h / 12 mois).
 * Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>OUVERTS : le seuil d'affiliation de 507 heures sur 12 mois est atteint —
 *       ouverture des droits à l'allocation d'aide au retour à l'emploi.</li>
 *   <li>NON_OUVERTS : le seuil de 507 heures n'est pas atteint — pas d'ouverture
 *       des droits ; le nombre d'heures manquantes est indiqué.</li>
 *   <li>A_VERIFIER : total d'heures dans la marge de ± 10 h du seuil — décompte à
 *       confirmer auprès de France Travail avant de conclure.</li>
 * </ul>
 */
public enum IntermittentSpectacleAreVerdict {
    OUVERTS,
    NON_OUVERTS,
    A_VERIFIER
}
