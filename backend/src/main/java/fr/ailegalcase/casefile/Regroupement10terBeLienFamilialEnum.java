package fr.ailegalcase.casefile;

/**
 * SF-215-03 : lien familial du membre de famille candidat au regroupement
 * familial au sens de l'art. 10 et 10ter Loi 15/12/1980 (ressortissant tiers
 * en séjour illimité — carte B ou C). 5 valeurs autorisées.
 *
 * <ul>
 *   <li>CONJOINT : mariage valide reconnu en droit belge.</li>
 *   <li>PARTENAIRE_ENREGISTRE : partenariat enregistré équivalent au mariage
 *       reconnu en droit belge (loi 23/11/1998 + AR 14/12/2013).</li>
 *   <li>ENFANT_MOINS_21 : descendant légal &lt; 21 ans (art. 10 §1 4°).</li>
 *   <li>ENFANT_21_PLUS_CHARGE : descendant légal ≥ 21 ans à charge du regroupant.</li>
 *   <li>ASCENDANT_CHARGE : parent à charge du regroupant ressortissant tiers
 *       en séjour illimité (cas limité, art. 10 §1 6°).</li>
 * </ul>
 */
public enum Regroupement10terBeLienFamilialEnum {
    CONJOINT,
    PARTENAIRE_ENREGISTRE,
    ENFANT_MOINS_21,
    ENFANT_21_PLUS_CHARGE,
    ASCENDANT_CHARGE
}
