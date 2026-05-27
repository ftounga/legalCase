package fr.ailegalcase.casefile;

/**
 * SF-215-05 : type de carte de séjour du regroupant ressortissant tiers
 * justifiant l'éligibilité au regroupement familial 10bis (séjour limité).
 *
 * <p>Une seule valeur autorisée — par construction, le 10bis ne concerne que
 * les regroupants en séjour limité. Les regroupants en séjour illimité
 * (cartes B / C) relèvent du 10ter (SF-215-03).
 *
 * <ul>
 *   <li>CARTE_A : titre de séjour limité dans le temps — typiquement
 *       autorisation de séjour provisoire d'1 an renouvelable (AR 08/10/1981
 *       art. 25, modifié AR 22/07/2008).</li>
 * </ul>
 */
public enum Regroupement10bisBeTypeCarteEnum {
    CARTE_A
}
