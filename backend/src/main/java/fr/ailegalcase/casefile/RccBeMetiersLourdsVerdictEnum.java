package fr.ailegalcase.casefile;

/**
 * SF-219-01 : verdict de l'outil d'éligibilité au RCC métiers lourds BE.
 *
 * <ul>
 *   <li>{@link #ELIGIBLE} — toutes les conditions cumulatives sont remplies
 *       (licenciement effectif ; âge ≥ 58 ; carrière ≥ 35 ; 5 ans métier
 *       lourd dans les 10 dernières années OU 7 ans dans les 15 dernières
 *       années). Le travailleur peut prétendre au RCC métiers lourds
 *       (CCT 17 + AR 03/05/2007 art. 3). L'indemnité complémentaire est
 *       calculée séparément par
 *       {@code rcc-be-indemnite-complementaire} (F-207 SF-207-07).</li>
 *   <li>{@link #INELIGIBLE} — au moins une condition manque. La raison
 *       précise est portée par {@link RccBeMetiersLourdsRaisonIneligibilite}.</li>
 * </ul>
 */
public enum RccBeMetiersLourdsVerdictEnum {
    /** Éligible — toutes conditions cumulatives remplies. */
    ELIGIBLE,

    /** Inéligible — au moins une condition manque (raison détaillée). */
    INELIGIBLE
}
