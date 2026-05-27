package fr.ailegalcase.casefile;

/**
 * SF-215-05 : lien familial du membre de famille candidat au regroupement
 * familial au sens de l'art. 10 et 10bis Loi 15/12/1980 (ressortissant tiers
 * en séjour limité — carte A). 5 valeurs autorisées — identiques à 10ter
 * (la seule différence 10bis vs 10ter porte sur la nature du titre de séjour
 * du regroupant, pas sur les liens familiaux ouvrant droit).
 *
 * <ul>
 *   <li>CONJOINT : mariage valide reconnu en droit belge.</li>
 *   <li>PARTENAIRE_ENREGISTRE : partenariat enregistré équivalent au mariage
 *       reconnu en droit belge (loi 23/11/1998 + AR 14/12/2013).</li>
 *   <li>ENFANT_MOINS_21 : descendant légal &lt; 21 ans (art. 10 §1 4°).</li>
 *   <li>ENFANT_21_PLUS_CHARGE : descendant légal ≥ 21 ans à charge du regroupant.</li>
 *   <li>ASCENDANT_CHARGE : parent à charge du regroupant ressortissant tiers
 *       en séjour limité (cas limité, art. 10 §1 6°).</li>
 * </ul>
 */
public enum Regroupement10bisBeLienFamilialEnum {
    CONJOINT,
    PARTENAIRE_ENREGISTRE,
    ENFANT_MOINS_21,
    ENFANT_21_PLUS_CHARGE,
    ASCENDANT_CHARGE
}
