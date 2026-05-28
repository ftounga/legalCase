package fr.ailegalcase.casefile;

/**
 * SF-219-26 : statut de la déclaration DIMONA (Déclaration Immédiate
 * de l'Emploi à l'ONSS) au moment du contrôle inspection sociale.
 *
 * <ul>
 *   <li>{@link #DECLAREE_AVANT_DEBUT} — DIMONA effectuée avant le
 *       début effectif de l'occupation du travailleur (cas nominal
 *       art. 4 AR du 05/11/2002 — obligation préalable).</li>
 *   <li>{@link #DECLAREE_TARDIVE} — DIMONA effectuée après le début
 *       effectif mais avant le contrôle inspection sociale —
 *       régularisation tardive possible, sanction administrative
 *       atténuée mais pas niveau 4 pénal.</li>
 *   <li>{@link #ABSENTE} — aucune DIMONA au moment du contrôle —
 *       infraction art. 181 C. pén. soc. niveau 4 + présomption
 *       de salariat art. 328 C. pén. soc. + cotisations
 *       rétroactives ONSS dues + amende ONSS forfaitaire 3×.</li>
 *   <li>{@link #INDEPENDANT_FAUSSEMENT_QUALIFIE} — relation
 *       faussement qualifiée d'indépendante sans DIMONA, à
 *       requalifier en salariat avec rappel cotisations ONSS
 *       depuis l'origine.</li>
 * </ul>
 */
public enum TravailNoirBeDimonaStatutDimona {
    /** DIMONA effectuée avant début occupation — cas nominal. */
    DECLAREE_AVANT_DEBUT,

    /** DIMONA effectuée après début mais avant contrôle. */
    DECLAREE_TARDIVE,

    /** Aucune DIMONA au moment du contrôle. */
    ABSENTE,

    /** Relation indépendante faussement qualifiée — à requalifier. */
    INDEPENDANT_FAUSSEMENT_QUALIFIE
}
