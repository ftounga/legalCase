package fr.ailegalcase.casefile;

/**
 * SF-215-15 : type d'acte exécutoire imminent justifiant un recours en extrême
 * urgence devant le Conseil du Contentieux des Étrangers (CCE) —
 * art. 39/82 §4 al. 2-3 de la Loi du 15/12/1980.
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> (droit des étrangers belge). Sans rapport
 * avec un dispositif de crédit à la consommation.
 */
public enum CceExtremeUrgenceBeTypeActeEnum {
    /** Ordre de quitter le territoire en cours d'exécution (mesure d'éloignement effective). */
    OQT_EXECUTE,
    /** Transfert Dublin imminent vers l'État membre responsable. */
    TRANSFERT_DUBLIN,
    /** Refus d'accès au territoire avec maintien / refoulement imminent (Annexe 11/25 quater). */
    REFUS_ACCES_TERRITOIRE,
    /** Expulsion / rapatriement programmé à très court terme. */
    EXPULSION_IMMEDIATE,
    /** Autre acte exécutoire imminent susceptible de recours en extrême urgence. */
    AUTRE
}
