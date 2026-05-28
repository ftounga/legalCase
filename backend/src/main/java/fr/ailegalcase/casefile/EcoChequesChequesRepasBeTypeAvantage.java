package fr.ailegalcase.casefile;

/**
 * SF-219-21 : type d'avantage extra-légal analysé par l'outil
 * <i>éco-chèques + chèques-repas BE</i>.
 *
 * <p>Les deux dispositifs relèvent de régimes juridiques distincts
 * mais sont rassemblés dans un même outil car ils sont couramment
 * négociés de pair en cabinet de droit social et partagent la même
 * mécanique de qualification (avantage extra-légal exonéré de
 * cotisations ONSS et d'impôt sur les revenus à la double condition
 * du respect du cadre légal et de l'absence de substitution salariale).</p>
 *
 * <ul>
 *   <li>{@link #ECO_CHEQUES} — CCT n°98 du CNT (Conseil National du
 *       Travail) du 20/02/2009, rendue obligatoire par AR du 12/04/2009
 *       (M.B. 06/05/2009). Plafond : 250 EUR/an/travailleur, validité
 *       2 ans, secteurs sans CCT sectorielle = pas d'éco-chèques par
 *       défaut (besoin d'une CCT d'entreprise ou d'une convention
 *       individuelle écrite art. 3 CCT n°98).</li>
 *   <li>{@link #CHEQUES_REPAS} — Loi du 25/04/2014 portant des
 *       dispositions diverses + AR du 03/02/2010 (art. 19bis arrêté
 *       royal du 28/11/1969 sur la sécurité sociale). Plafond
 *       d'exonération : 6,91 EUR/jour effectivement presté (montant
 *       facial maximal 8 EUR), contribution travailleur minimale
 *       1,09 EUR, conditions cumulatives (1 chèque/jour, pas de
 *       cumul avec frais de bouche, paiement électronique obligatoire
 *       depuis 01/01/2016).</li>
 *   <li>{@link #INDETERMINE} — type d'avantage non précisé.</li>
 * </ul>
 */
public enum EcoChequesChequesRepasBeTypeAvantage {
    /** Éco-chèques — CCT n°98 du CNT du 20/02/2009. */
    ECO_CHEQUES,

    /** Chèques-repas — Loi 25/04/2014 + AR 03/02/2010. */
    CHEQUES_REPAS,

    /** Type non précisé — A_ANALYSER. */
    INDETERMINE
}
