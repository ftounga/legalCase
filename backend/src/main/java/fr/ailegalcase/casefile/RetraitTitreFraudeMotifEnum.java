package fr.ailegalcase.casefile;

/**
 * SF-214-41 : motif d'un retrait de titre de séjour pour fraude
 * (art. L. 412-7 CESEDA). Le motif oriente les moyens de contestation.
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (droit des étrangers français).
 */
public enum RetraitTitreFraudeMotifEnum {
    /** Mariage gris / mariage de complaisance allégué par l'administration. */
    MARIAGE_GRIS,
    /** Fausses déclarations (état civil, ressources, résidence). */
    FAUSSES_DECLARATIONS,
    /** Production de documents falsifiés ou contrefaits. */
    FRAUDE_DOCUMENTAIRE,
    /** Perte des conditions de délivrance du titre (hors fraude stricto sensu). */
    PERTE_CONDITIONS
}
