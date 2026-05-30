package fr.ailegalcase.casefile;

/**
 * SF-218-09 : objet de l'action de groupe en discrimination (art. L. 1134-8
 * Code travail). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>CESSATION_MANQUEMENT : faire cesser le manquement (discrimination
 *       collective) pour l'avenir.</li>
 *   <li>REPARATION_PREJUDICES : obtenir la réparation des préjudices subis.</li>
 *   <li>LES_DEUX : cessation du manquement et réparation des préjudices
 *       (défaut).</li>
 * </ul>
 */
public enum ActionGroupeDiscriminationObjet {
    CESSATION_MANQUEMENT,
    REPARATION_PREJUDICES,
    LES_DEUX
}
