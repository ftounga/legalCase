package fr.ailegalcase.casefile;

/**
 * SF-215-03 : type de carte de séjour du regroupant ressortissant tiers
 * justifiant l'éligibilité au regroupement familial 10ter (séjour illimité).
 *
 * <ul>
 *   <li>CARTE_B : carte d'identité d'étranger — séjour illimité depuis l'AR
 *       du 22/07/2008. Synonyme historique : ancienne CIRE.</li>
 *   <li>CARTE_C : carte d'identité d'étranger établi — séjour illimité avec
 *       droits étendus, équivalent du résident longue durée européen.</li>
 * </ul>
 *
 * Seuls ces deux titres ouvrent le regroupement 10ter — les cartes A
 * (séjour limité) relèvent du 10bis (SF-215-05).
 */
public enum Regroupement10terBeTypeCarteEnum {
    CARTE_B,
    CARTE_C
}
