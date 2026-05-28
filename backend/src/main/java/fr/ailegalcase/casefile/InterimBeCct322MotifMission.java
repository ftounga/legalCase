package fr.ailegalcase.casefile;

/**
 * SF-219-14 : motif justifiant le recours au travail intérimaire
 * (Loi du 24/07/1987, art. 1er § 1 et art. 1bis ; CCT n° 108 du
 * 16/07/2013 pour le motif insertion en vue d'embauche durable).
 *
 * <p>La liste des motifs est <b>limitative</b>. Tout autre motif
 * entraîne la requalification immédiate en CDI utilisateur
 * (art. 20 Loi 24/07/1987 + CCT n° 322).</p>
 *
 * <h2>Motifs autorisés</h2>
 * <ul>
 *   <li>{@link #REMPLACEMENT_TRAVAILLEUR_PERMANENT} — remplacement
 *       d'un travailleur permanent dont le contrat est suspendu
 *       (maladie, congé, accident, maternité, ...) ou rompu, sauf
 *       grève / lock-out (cf. {@link InterimBeCct322Verdict}
 *       INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT). Durée = durée
 *       de la suspension + 7 jours après reprise (art. 8 § 2).</li>
 *   <li>{@link #SURCROIT_TEMPORAIRE_DE_TRAVAIL} — accroissement
 *       temporaire d'activité. Durée max 6 mois renouvelables 1
 *       fois pour un total de 12 mois (CCT n° 108 / accord
 *       d'entreprise + autorisation délégation syndicale au-delà
 *       de 6 mois).</li>
 *   <li>{@link #EXECUTION_TRAVAIL_EXCEPTIONNEL} — liste limitative
 *       AR du 11/10/1976 : inventaires, déménagement d'établissement,
 *       foires, expositions, événements culturels / sportifs, ...
 *       Durée énumérée par l'AR (ex. 3 mois pour inventaires).</li>
 *   <li>{@link #INSERTION_EMBAUCHE_DURABLE} — motif d'insertion en
 *       vue d'embauche durable, introduit par la Loi du 26/06/2013
 *       (art. 1bis Loi 24/07/1987 + CCT n° 108). Max 3 tentatives,
 *       6 mois cumulés par tentative, 9 mois cumulés au total.</li>
 *   <li>{@link #MOTIF_ARTISTIQUE} — régime art. 1bis § 4 :
 *       spectacle vivant, production audiovisuelle, intermittents
 *       du spectacle. Durée variable selon production.</li>
 *   <li>{@link #FLUX_TRAVAUX_EXCEPTIONNEL_TRAJET_SCOLAIRE} — cas
 *       étroits relevant d'AR sectoriels (transport scolaire,
 *       flux saisonniers très spécifiques). À confirmer cas par
 *       cas par avocat BE.</li>
 *   <li>{@link #AUTRE_NON_AUTORISE} — tout autre motif → rejet
 *       immédiat + requalification CDI utilisateur.</li>
 * </ul>
 */
public enum InterimBeCct322MotifMission {
    /** Remplacement d'un travailleur permanent (suspension / rupture contrat hors grève). */
    REMPLACEMENT_TRAVAILLEUR_PERMANENT,

    /** Surcroît temporaire d'activité — 6 mois renouvelables 1 fois (max 12 mois). */
    SURCROIT_TEMPORAIRE_DE_TRAVAIL,

    /** Exécution d'un travail exceptionnel listé par l'AR 11/10/1976. */
    EXECUTION_TRAVAIL_EXCEPTIONNEL,

    /** Insertion en vue d'embauche durable — CCT n° 108 (max 9 mois cumulés, 3 tentatives). */
    INSERTION_EMBAUCHE_DURABLE,

    /** Motif artistique (spectacle / audiovisuel) — art. 1bis § 4. */
    MOTIF_ARTISTIQUE,

    /** Flux de travaux exceptionnels / trajet scolaire — AR sectoriels étroits. */
    FLUX_TRAVAUX_EXCEPTIONNEL_TRAJET_SCOLAIRE,

    /** Motif non listé par la loi — recours illégal, requalification immédiate. */
    AUTRE_NON_AUTORISE
}
