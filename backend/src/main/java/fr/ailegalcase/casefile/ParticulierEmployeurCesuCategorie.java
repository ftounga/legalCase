package fr.ailegalcase.casefile;

/**
 * SF-218-13 : catégorie du salarié du particulier employeur — pilote la CCN
 * applicable au calcul du préavis et de l'indemnité de licenciement. Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>SALARIE_PARTICULIER_EMPLOYEUR : employé de maison, garde d'enfants à
 *       domicile — CCN des salariés du particulier employeur (IDCC 3239, 2021),
 *       art. L. 7221-1 et s. CT.</li>
 *   <li>ASSISTANT_MATERNEL : assistant·e maternel·le agréé·e accueillant
 *       l'enfant à son domicile — CCN des assistants maternels du particulier
 *       employeur, art. L. 423-1 et s. CASF. Indemnité de rupture spécifique
 *       (1/80 des salaires nets perçus).</li>
 * </ul>
 */
public enum ParticulierEmployeurCesuCategorie {
    SALARIE_PARTICULIER_EMPLOYEUR,
    ASSISTANT_MATERNEL
}
