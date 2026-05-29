package fr.ailegalcase.casefile;

/**
 * SF-215-13 : type de décision de l'Office des Étrangers (OE) ou du Commissariat
 * Général aux Réfugiés et aux Apatrides (CGRA) faisant l'objet d'un recours en
 * annulation devant le Conseil du Contentieux des Étrangers (CCE) — art. 39/2 §2
 * et 39/82 §4 al. 1 de la Loi du 15/12/1980.
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> (droit des étrangers belge). Sans rapport
 * avec un quelconque dispositif de crédit à la consommation.
 */
public enum CceAnnulationBeTypeDecisionEnum {
    /** Refus / retrait d'un titre de séjour. */
    REFUS_TITRE,
    /** Refus d'une demande de regroupement familial (art. 10, 10bis, 10ter, 40bis, 40ter). */
    REFUS_REGROUPEMENT,
    /** Refus d'une autorisation de séjour pour circonstances exceptionnelles (art. 9bis). */
    REFUS_9BIS,
    /** Refus d'une autorisation de séjour pour raisons médicales (art. 9ter). */
    REFUS_9TER,
    /** Ordre de quitter le territoire (Annexe 13). */
    OQT_ANNEXE13,
    /** Décision du CGRA en matière de protection internationale (recours suspensif CCE). */
    DECISION_CGRA,
    /** Autre décision de l'OE susceptible de recours en annulation. */
    AUTRE
}
