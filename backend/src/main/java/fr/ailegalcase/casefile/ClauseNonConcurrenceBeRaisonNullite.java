package fr.ailegalcase.casefile;

/**
 * SF-213-01 : raison de nullité (totale ou partielle) — null si verdict VALIDE.
 */
public enum ClauseNonConcurrenceBeRaisonNullite {
    /** Rémunération annuelle brute insuffisante (≤ seuil). */
    REMUNERATION_INSUFFISANTE,
    /** Durée &gt; 12 mois (plafond légal art. 65 §2). */
    DUREE_EXCESSIVE,
    /** Zone géographique étendue à l'étranger sans activité internationale prouvée. */
    ZONE_GEOGRAPHIQUE_NON_JUSTIFIEE,
    /** Zone géographique non renseignée dans la clause. */
    ZONE_GEOGRAPHIQUE_NON_SPECIFIEE
}
