package fr.ailegalcase.casefile;

/**
 * SF-213-07 : étape de la procédure interne de plainte pour harcèlement
 * moral / sexuel en droit belge.
 *
 * <p>Source : Loi du 4 août 1996 art. 32bis–32sexies + AR du 10 avril 2014
 * relatif à la prévention des risques psychosociaux au travail.</p>
 *
 * <h2>Cycle procédural</h2>
 * <ol>
 *   <li>{@link #AVANT_DEPOT} — pas de plainte déposée. Travailleur s'oriente
 *       (identifier le CPAP, choisir voie informelle ou formelle,
 *       consigner les faits).</li>
 *   <li>{@link #DEMANDE_INFORMELLE_EN_COURS} — démarche informelle auprès du
 *       CPAP / personne de confiance — pas de délai légal strict.</li>
 *   <li>{@link #DEMANDE_FORMELLE_EN_COURS} — plainte formelle déposée,
 *       enquête CPAP en cours — délai 90 jours (AR 10/04/2014).</li>
 *   <li>{@link #ENQUETE_TERMINEE} — enquête achevée, examen des mesures
 *       employeur, décision recours tribunal du travail.</li>
 *   <li>{@link #MESURE_DEFAVORABLE_APRES_PLAINTE} — mesure défavorable
 *       observée après dépôt — présomption de représailles (art. 32sexies),
 *       protection 12 mois.</li>
 * </ol>
 */
public enum HarcelementBeEtapeEnum {
    /** Aucune plainte déposée — orientation pré-démarche. */
    AVANT_DEPOT,

    /** Demande informelle (intervention CPAP / personne de confiance) en cours. */
    DEMANDE_INFORMELLE_EN_COURS,

    /** Demande formelle déposée — enquête CPAP en cours (90 jours). */
    DEMANDE_FORMELLE_EN_COURS,

    /** Enquête CPAP terminée — examen mesures employeur / recours. */
    ENQUETE_TERMINEE,

    /** Mesure défavorable post-plainte — présomption représailles. */
    MESURE_DEFAVORABLE_APRES_PLAINTE
}
