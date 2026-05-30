package fr.ailegalcase.casefile;

/**
 * SF-218-13 : méthode de calcul de l'indemnité de licenciement retenue selon la
 * catégorie du salarié du particulier employeur. Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>CONVENTIONNEL_PE : indemnité de licenciement conventionnelle de la CCN
 *       des salariés du particulier employeur (2021) — 1/4 de mois de salaire
 *       moyen par année d'ancienneté pour les 10 premières années, 1/3 au-delà
 *       (aligné sur R. 1234-2 CT).</li>
 *   <li>INDEMNITE_RUPTURE_ASSMAT : indemnité de rupture des assistants
 *       maternels — 1/80 du total des salaires nets perçus depuis le 1er jour
 *       du contrat (formule conventionnelle spécifique).</li>
 *   <li>AUCUNE : aucune indemnité due (faute grave / ancienneté insuffisante).</li>
 * </ul>
 */
public enum ParticulierEmployeurCesuMethode {
    CONVENTIONNEL_PE,
    INDEMNITE_RUPTURE_ASSMAT,
    AUCUNE
}
