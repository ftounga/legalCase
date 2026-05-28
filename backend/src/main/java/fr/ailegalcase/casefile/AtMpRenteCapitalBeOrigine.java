package fr.ailegalcase.casefile;

/**
 * SF-219-29 : origine de l'incapacite permanente partielle - point
 * d'entree de l'analyse rente vs capitalisation BE.
 *
 * <p>Le calcul du mode de versement (rente annuelle vs capital) est
 * identique pour les AT et MP - les Lois coordonnees du 03/06/1970
 * art. 35 renvoient pour les MP au regime AT (Loi du 10/04/1971
 * art. 24 + AR 21/12/1971 + AR 24/02/2005). Le drapeau d'origine
 * sert uniquement a alimenter le verdict (intitule + voies de recours
 * : C4-ACCIDENT vs C4-MALADIE, qualification d'expertise medicale
 * Fedris).</p>
 *
 * <ul>
 *   <li>{@link #ACCIDENT_TRAVAIL} - AT au sens art. 7 Loi 10/04/1971
 *       (evenement soudain, anormal et exterieur, dans le cours et
 *       par le fait de l'execution du contrat de travail) ou accident
 *       sur le chemin du travail art. 8.</li>
 *   <li>{@link #MALADIE_PROFESSIONNELLE} - MP reconnue par Fedris au
 *       sens des Lois coordonnees 03/06/1970 (liste fermee AR
 *       28/03/1969 ou systeme ouvert art. 30bis) - couplable avec
 *       l'outil SF-219-28 mp-fedris-reconnaissance.</li>
 * </ul>
 */
public enum AtMpRenteCapitalBeOrigine {
    /** Accident du travail Loi 10/04/1971 art. 7-8. */
    ACCIDENT_TRAVAIL,

    /** Maladie professionnelle Lois coordonnees 03/06/1970. */
    MALADIE_PROFESSIONNELLE
}
