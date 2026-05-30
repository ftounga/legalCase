package fr.ailegalcase.casefile;

/**
 * SF-218-17 : statut d'ouverture des droits à l'ARE pour l'intermittent du
 * spectacle (seuil d'affiliation de 507 heures sur 12 mois — annexes 8 et 10
 * Unedic). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>DROITS_OUVERTS : total d'heures retenues ≥ 507 h et au-delà de la marge
 *       de vérification — l'affiliation est acquise.</li>
 *   <li>DROITS_NON_OUVERTS : total d'heures retenues &lt; 507 h et en deçà de la
 *       marge de vérification — l'affiliation n'est pas acquise.</li>
 *   <li>A_VERIFIER : total d'heures retenues dans la marge de ± 10 h du seuil —
 *       vérification recommandée auprès de France Travail (le décompte exact des
 *       heures déclarées et des assimilations peut faire basculer le verdict).</li>
 * </ul>
 */
public enum IntermittentSpectacleAreStatut {
    DROITS_OUVERTS,
    DROITS_NON_OUVERTS,
    A_VERIFIER
}
