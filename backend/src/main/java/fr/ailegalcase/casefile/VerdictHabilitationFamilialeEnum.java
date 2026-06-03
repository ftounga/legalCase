package fr.ailegalcase.casefile;

/**
 * SF-222-03 : verdict 3 niveaux de l'outil habilitation familiale
 * (art. 494-1 à 494-12 Cciv). Famille FR uniquement.
 *
 * <p>L'habilitation familiale est l'alternative simplifiée à une mesure
 * judiciaire de protection (curatelle / tutelle — F-FA-25) lorsqu'un consensus
 * familial existe. L'outil évalue ses conditions propres ; le prononcé relève
 * du juge des contentieux de la protection.</p>
 */
public enum VerdictHabilitationFamilialeEnum {
    /** Conditions réunies pour une habilitation familiale générale (art. 494-6 Cciv). */
    ELIGIBLE_HABILITATION_GENERALE,
    /** Conditions réunies pour une habilitation familiale spéciale (un ou plusieurs actes déterminés, art. 494-1 Cciv). */
    ELIGIBLE_HABILITATION_SPECIALE,
    /**
     * Conditions de l'habilitation familiale non réunies (altération non constatée,
     * lien familial inéligible ou absence de consensus familial) — orienter vers une
     * mesure judiciaire de protection (sauvegarde / curatelle / tutelle — outil F-FA-25).
     */
    ORIENTER_VERS_MESURE_JUDICIAIRE
}
