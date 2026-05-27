package fr.ailegalcase.casefile;

/**
 * SF-219-01 : raison précise d'inéligibilité au RCC métiers lourds BE
 * lorsque le verdict de
 * {@link RccBeMetiersLourdsValidator#analyze(RccBeMetiersLourdsRequest)} est
 * {@link RccBeMetiersLourdsVerdictEnum#INELIGIBLE}.
 *
 * <p>L'ordre d'évaluation dans le validateur est fixé pour garantir un
 * message stable et exploitable côté UI :
 * <ol>
 *   <li>{@link #DEMISSION_NON_ELIGIBLE} — l'employeur n'a pas licencié.</li>
 *   <li>{@link #AGE_INSUFFISANT} — âge &lt; 58 à la fin du contrat.</li>
 *   <li>{@link #CARRIERE_INSUFFISANTE} — carrière totale &lt; 35 ans.</li>
 *   <li>{@link #DUREE_METIER_LOURD_INSUFFISANTE} — ni 5/10 ni 7/15.</li>
 * </ol>
 */
public enum RccBeMetiersLourdsRaisonIneligibilite {

    /** Démission — le RCC est strictement réservé au licenciement. */
    DEMISSION_NON_ELIGIBLE,

    /** Âge &lt; 58 ans à la fin du contrat. */
    AGE_INSUFFISANT,

    /** Carrière professionnelle totale &lt; 35 ans. */
    CARRIERE_INSUFFISANTE,

    /** Ni 5 ans en métier lourd sur les 10 dernières années, ni 7/15. */
    DUREE_METIER_LOURD_INSUFFISANTE
}
