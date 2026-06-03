package fr.ailegalcase.casefile;

/**
 * SF-222-04 : verdict 4 niveaux de l'outil assistance éducative — mineur en
 * danger (art. 375 et s. Cciv). Famille FR uniquement.
 *
 * <p>L'assistance éducative protège un mineur dont la santé, la sécurité ou la
 * moralité sont en danger, ou dont les conditions d'éducation ou le
 * développement physique, affectif, intellectuel et social sont gravement
 * compromis (art. 375 Cciv). UN SEUL outil oriente vers les 4 issues d'UNE même
 * situation, selon le degré de danger, l'urgence, l'adhésion de la famille et la
 * possibilité de maintien dans le milieu familial.</p>
 */
public enum VerdictAssistanceEducativeEnum {
    /**
     * AED — aide éducative à domicile (mesure administrative ASE), accord
     * parental, contractualisée (art. L. 222-3 CASF). Pas d'intervention du juge.
     */
    AED,
    /**
     * AEMO — action éducative en milieu ouvert (mesure judiciaire, juge des
     * enfants, art. 375-2 Cciv). Le mineur est maintenu dans son milieu familial.
     */
    AEMO,
    /**
     * OPP / placement — ordonnance de placement provisoire / placement (art.
     * 375-3 / 375-5 Cciv). Retrait du milieu familial, urgence ou maintien
     * impossible.
     */
    OPP_PLACEMENT,
    /**
     * Pas de mesure d'assistance éducative — le danger caractérisé de l'art. 375
     * n'est pas établi.
     */
    PAS_DE_MESURE
}
