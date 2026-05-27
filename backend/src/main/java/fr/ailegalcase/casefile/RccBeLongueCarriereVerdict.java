package fr.ailegalcase.casefile;

/**
 * SF-219-02 : verdict de l'outil RCC BE longue carrière — détermine si le
 * travailleur est éligible au régime de chômage avec complément d'entreprise
 * (RCC, ex-prépension) au titre de la <b>longue carrière</b>
 * (Loi du 26/12/2013 sur le pacte de solidarité entre les générations +
 * CCT n° 17 + AR du 03/05/2007 art. 3).
 *
 * <p>Outil <b>BE-only</b> distinct du RCC général (60+/40 — F-207 SF-207-06)
 * et du RCC métiers lourds (58+/35 + métier lourd — SF-219-01).</p>
 *
 * <ul>
 *   <li>{@link #ELIGIBLE_RCC_LONGUE_CARRIERE} — toutes les conditions
 *       cumulatives sont remplies (âge ≥ 59 ans à la fin du contrat,
 *       carrière ≥ 40 ans, licenciement effectif). Le travailleur peut
 *       bénéficier d'une allocation de chômage majorée d'une indemnité
 *       complémentaire à charge de l'employeur (ONEM gère la composante
 *       chômage).</li>
 *   <li>{@link #INELIGIBLE_AGE_INSUFFISANT} — âge à la fin du contrat
 *       strictement inférieur à 59 ans.</li>
 *   <li>{@link #INELIGIBLE_CARRIERE_INSUFFISANTE} — carrière professionnelle
 *       (jours équivalents temps plein) strictement inférieure à 40 ans.</li>
 *   <li>{@link #INELIGIBLE_DEMISSION} — le travailleur a démissionné : le
 *       RCC est par définition réservé au licenciement à l'initiative de
 *       l'employeur (préavis presté ou indemnité compensatoire).</li>
 * </ul>
 */
public enum RccBeLongueCarriereVerdict {
    /** Toutes conditions cumulatives remplies — RCC longue carrière ouvert. */
    ELIGIBLE_RCC_LONGUE_CARRIERE,

    /** Âge à la fin du contrat < 59 ans → inéligible. */
    INELIGIBLE_AGE_INSUFFISANT,

    /** Carrière professionnelle < 40 ans → inéligible. */
    INELIGIBLE_CARRIERE_INSUFFISANTE,

    /** Démission : le RCC ne s'applique qu'aux licenciements effectifs. */
    INELIGIBLE_DEMISSION
}
