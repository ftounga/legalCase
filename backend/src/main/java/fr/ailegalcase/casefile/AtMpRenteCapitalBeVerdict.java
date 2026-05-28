package fr.ailegalcase.casefile;

/**
 * SF-219-29 : verdict de l'outil <i>Rente AT/MP vs capitalisation
 * BE</i> (Loi du 10/04/1971 sur les accidents du travail art. 24 ;
 * Lois coordonnees du 03/06/1970 art. 35 pour les maladies
 * professionnelles - renvoi au regime AT ; AR du 21/12/1971 portant
 * execution de la Loi 10/04/1971 - bareme de capitalisation et
 * coefficients d'age ; AR du 10/12/1987 fixant les regles de
 * conversion partielle de la rente en capital ; AR du 24/02/2005
 * actualisant le bareme).
 *
 * <ul>
 *   <li>{@link #CAPITAL_FORFAITAIRE_LT_19} - taux IPP &lt; 19% :
 *       l'indemnite est versee en CAPITAL UNIQUE (capitalisation
 *       d'office art. 24 al. 2 Loi 10/04/1971 ; art. 4 AR
 *       10/12/1987). Pas de rente periodique. Montant calcule selon
 *       le bareme AR 21/12/1971 actualise AR 24/02/2005, fonction de
 *       la remuneration de base, du taux IPP et de l'age de la
 *       victime au moment de la consolidation.</li>
 *   <li>{@link #RENTE_ANNUELLE_GE_19} - taux IPP &gt;= 19% : la
 *       reparation est versee en RENTE ANNUELLE viagere (art. 24 al.
 *       1 Loi 10/04/1971). Base = remuneration de base x taux IPP.
 *       Possibilite de conversion partielle (1/3 max) en capital
 *       apres 3 ans (art. 45ter Loi 10/04/1971 + AR
 *       10/12/1987).</li>
 *   <li>{@link #INELIGIBLE_NON_RECONNU} - AT ou MP non reconnu
 *       Fedris / assureur-loi : ni rente ni capital ne sont dus tant
 *       que la reconnaissance n'est pas acquise. Voie : recours
 *       tribunal du travail art. 580 1° C. jud., delai 1 an art. 64
 *       Loi 10/04/1971 / art. 23 lois coordonnees 03/06/1970.</li>
 *   <li>{@link #IPP_NON_DETERMINE} - taux d'incapacite permanente
 *       partielle pas encore arrete par l'expertise medicale Fedris
 *       ou assureur-loi (consolidation non acquise, expertise en
 *       cours) : calcul prematuré, attendre la decision de
 *       consolidation art. 24 § 2 AR 03/07/1996.</li>
 *   <li>{@link #A_QUALIFIER} - inputs ambigus (remuneration de base
 *       manquante ou negative, age &lt; 16 ou &gt; 75, statut de
 *       reconnaissance ambigu) - analyse complementaire avocat
 *       requise.</li>
 * </ul>
 *
 * <h2>Articulation FR / BE</h2>
 * <p>L'equivalent FR (rente AT/MP CPAM art. L. 434-1 et s. CSS) repose
 * sur un mecanisme distinct : pas de seuil 19% de basculement
 * rente/capital BE, mais bareme de gravite (&lt; 10% indemnite en
 * capital art. L. 434-1 ; 10-50% rente reduite ; &gt;= 50% rente
 * pleine), bareme indicatif art. R. 434-2 differemment construit,
 * recours TASS / Pole social TJ et non tribunal du travail.
 * Restitution separee par outil - pas d'harmonisation FR/BE.</p>
 */
public enum AtMpRenteCapitalBeVerdict {
    /** IPP &lt; 19% - capital forfaitaire unique. */
    CAPITAL_FORFAITAIRE_LT_19,

    /** IPP &gt;= 19% - rente annuelle viagere. */
    RENTE_ANNUELLE_GE_19,

    /** AT ou MP non reconnu - ni rente ni capital. */
    INELIGIBLE_NON_RECONNU,

    /** IPP pas encore arrete - calcul premature. */
    IPP_NON_DETERMINE,

    /** Inputs ambigus - analyse complementaire requise. */
    A_QUALIFIER
}
