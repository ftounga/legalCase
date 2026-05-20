package fr.ailegalcase.casefile;

/**
 * SF-207-06 : les 4 régimes parallèles du Régime de Chômage avec Complément
 * d'entreprise (RCC, ex-prépension) en droit belge.
 *
 * <p>Chaque régime impose des seuils d'âge et de carrière distincts (cf.
 * {@link RccBeConditionsCalculator}) — un dossier peut cumuler plusieurs
 * régimes éligibles ; dans ce cas, {@code regimeApplicable} est déterminé
 * par la priorité :
 * <ol>
 *   <li>{@link #GENERAL} — régime de droit commun (CCT 17), le plus stable
 *       juridiquement ;</li>
 *   <li>{@link #LONGUE_CARRIERE} — variante régime général à 59 ans pour les
 *       très longues carrières (CCT 17 art. 3) ;</li>
 *   <li>{@link #METIERS_LOURDS} — CCT 17/13 (métiers lourds : travail de
 *       nuit, équipes successives, BTP, etc.) ;</li>
 *   <li>{@link #ENTREPRISE_DIFFICULTE} — AR du 03/05/2007 art. 8 (entreprise
 *       reconnue en difficulté), régime conjoncturel le moins stable.</li>
 * </ol>
 *
 * <p>Outil <b>BE-only</b> — substance juridique BE pure (mémoire
 * {@code feedback_belgique_never_forget}). Le régime FR de cessation
 * anticipée d'activité n'a pas d'équivalent direct (les régimes français
 * d'aménagement de fin de carrière relèvent d'autres dispositifs).</p>
 */
public enum RccBeConditionsRegime {

    /** Régime général — âge ≥ 60 ans ET carrière ≥ 40 ans (CCT 17 art. 3). */
    GENERAL,

    /**
     * Régime métiers lourds — âge ≥ 58 ans ET carrière ≥ 35 ans ET métier
     * lourd reconnu (CCT 17/13).
     */
    METIERS_LOURDS,

    /**
     * Régime longue carrière — âge ≥ 59 ans ET carrière ≥ 40 ans (CCT 17
     * art. 3, variante longue carrière).
     */
    LONGUE_CARRIERE,

    /**
     * Régime entreprise en difficulté — âge ≥ 60 ans ET entreprise
     * reconnue en difficulté par arrêté ministériel (AR du 03/05/2007
     * art. 8).
     */
    ENTREPRISE_DIFFICULTE
}
