package fr.ailegalcase.casefile;

/**
 * SF-219-29 : statut de reconnaissance par Fedris de l'AT ou de la MP
 * a l'origine de l'incapacite permanente partielle.
 *
 * <p>La rente ou le capital ne peuvent etre calcules qu'apres
 * reconnaissance prealable de l'AT (decision de l'assureur-loi
 * employeur, regulee par Fedris) ou de la MP (decision Fedris art.
 * 23 lois coordonnees 03/06/1970). Tant que la reconnaissance n'est
 * pas acquise, le calcul reste indicatif et ne peut donner lieu a
 * versement effectif.</p>
 *
 * <ul>
 *   <li>{@link #RECONNU} - decision de reconnaissance acquise, taux
 *       IPP arrete par l'expertise medicale Fedris ou
 *       assureur-loi.</li>
 *   <li>{@link #CONTESTE} - reconnaissance refusee ou taux conteste -
 *       voie de recours tribunal du travail art. 580 1° C. jud.,
 *       delai 1 an Loi 10/04/1971 art. 64 / Lois coordonnees art.
 *       23.</li>
 *   <li>{@link #EN_COURS} - dossier en instruction Fedris /
 *       assureur-loi - calcul indicatif, ne pas verser.</li>
 * </ul>
 */
public enum AtMpRenteCapitalBeStatutReconnaissance {
    /** Reconnaissance acquise - calcul effectif possible. */
    RECONNU,

    /** Refus ou taux conteste - tribunal du travail. */
    CONTESTE,

    /** Instruction en cours - calcul indicatif uniquement. */
    EN_COURS
}
