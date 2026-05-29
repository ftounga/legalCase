package fr.ailegalcase.casefile;

/**
 * SF-215-17 : motif de l'interdiction d'entrée prononcée dans une Annexe 13quinquies
 * (OQT + interdiction d'entrée — art. 74/11 de la Loi du 15/12/1980 sur l'accès au
 * territoire, le séjour, l'établissement et l'éloignement des étrangers).
 *
 * <p>Le motif détermine la durée de l'interdiction d'entrée (3 / 5 / 8 ans —
 * art. 74/11 §2). Outil <b>BELGIQUE UNIQUEMENT</b> (droit des étrangers belge).
 *
 * <p>Sources : Loi 15/12/1980 art. 74/11 (interdiction d'entrée), 74/12 (levée) ;
 * AR 08/10/1981 (Annexe 13quinquies).
 */
public enum Annexe13quinquiesBeMotifEnum {
    /** Séjour irrégulier sans délai de départ volontaire respecté / accordé. */
    SEJOUR_IRREGULIER,
    /** Menace pour l'ordre public. */
    MENACE_ORDRE_PUBLIC,
    /** Raisons de sécurité nationale. */
    RAISONS_SECURITE_NATIONALE,
    /** Atteinte grave à l'intérêt de l'Union européenne. */
    ATTEINTE_INTERET_UE,
    /** Interdiction d'entrée assortissant une décision judiciaire. */
    DECISION_JUDICIAIRE
}
