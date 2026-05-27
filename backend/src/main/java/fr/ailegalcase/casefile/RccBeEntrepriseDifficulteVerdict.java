package fr.ailegalcase.casefile;

/**
 * SF-219-03 : verdict de l'outil RCC BE entreprise en difficulté /
 * restructuration — détermine si le travailleur est éligible au régime de
 * chômage avec complément d'entreprise (RCC, ex-prépension) au titre du
 * régime <b>entreprise reconnue en difficulté ou en restructuration</b>
 * (Loi du 26/12/2013 sur le pacte de solidarité entre les générations +
 * CCT n° 17 du 19/12/1974 + AR du 03/05/2007 + AR de reconnaissance).
 *
 * <p>Outil <b>BE-only</b> distinct du RCC général (60+/40 — F-207
 * SF-207-06), du RCC métiers lourds (58+/35 + métier lourd — SF-219-01)
 * et du RCC longue carrière (59+/40 — SF-219-02). La singularité de ce
 * régime tient à la <b>reconnaissance ministérielle préalable</b> de
 * l'entreprise — sans cette reconnaissance, aucune dérogation d'âge
 * n'est ouverte.</p>
 *
 * <ul>
 *   <li>{@link #ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE} — toutes les
 *       conditions cumulatives sont remplies (reconnaissance ministre,
 *       âge ≥ seuil réduit du plan, carrière ≥ 10 ans, ancienneté
 *       secteur ≥ 5 ans, licenciement effectif).</li>
 *   <li>{@link #INELIGIBLE_DEMISSION} — la démission est exclue du RCC
 *       (CCT n° 17 art. 3).</li>
 *   <li>{@link #INELIGIBLE_RECONNAISSANCE_ABSENTE} — l'entreprise n'a
 *       pas été reconnue en difficulté ou en restructuration par le
 *       ministre de l'Emploi : sans cette reconnaissance, aucun régime
 *       dérogatoire RCC entreprise difficulté n'est ouvert.</li>
 *   <li>{@link #INELIGIBLE_AGE_INSUFFISANT} — âge à la fin du contrat
 *       strictement inférieur à l'âge réduit fixé par le plan reconnu.</li>
 *   <li>{@link #INELIGIBLE_CARRIERE_INSUFFISANTE} — carrière
 *       professionnelle totale strictement inférieure à 10 ans (jours
 *       équivalents temps plein).</li>
 *   <li>{@link #INELIGIBLE_ANCIENNETE_INSUFFISANTE} — ancienneté
 *       sectorielle / dans l'entreprise reconnue strictement inférieure
 *       à 5 ans.</li>
 * </ul>
 */
public enum RccBeEntrepriseDifficulteVerdict {

    /** Toutes conditions cumulatives remplies — RCC entreprise difficulté ouvert. */
    ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE,

    /** Démission : le RCC ne s'applique qu'aux licenciements effectifs. */
    INELIGIBLE_DEMISSION,

    /** Aucune reconnaissance ministérielle : pas d'ouverture du régime dérogatoire. */
    INELIGIBLE_RECONNAISSANCE_ABSENTE,

    /** Âge à la fin du contrat < seuil réduit du plan reconnu → inéligible. */
    INELIGIBLE_AGE_INSUFFISANT,

    /** Carrière professionnelle < 10 ans → inéligible. */
    INELIGIBLE_CARRIERE_INSUFFISANTE,

    /** Ancienneté secteur / entreprise reconnue < 5 ans → inéligible. */
    INELIGIBLE_ANCIENNETE_INSUFFISANTE
}
