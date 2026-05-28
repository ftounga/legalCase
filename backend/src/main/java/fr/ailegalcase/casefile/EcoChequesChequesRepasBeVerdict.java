package fr.ailegalcase.casefile;

/**
 * SF-219-21 : verdict de l'outil <i>éco-chèques + chèques-repas BE</i>
 * (CCT n°98 du CNT du 20/02/2009 + Loi du 25/04/2014 portant des
 * dispositions diverses + AR du 03/02/2010 modifiant l'art. 19bis de
 * l'AR du 28/11/1969 d'exécution de la loi sur la sécurité sociale).
 *
 * <h2>Hiérarchie</h2>
 * <ul>
 *   <li>{@link #CONFORME_EXONERATION_INTEGRALE} — l'avantage est
 *       correctement attribué (plafond et conditions respectés),
 *       intégralement exonéré de cotisations ONSS et d'impôt.</li>
 *   <li>{@link #CONFORME_PARTIELLEMENT_EXONERE} — le plafond légal
 *       est respecté pour la fraction conforme ; la part excédentaire
 *       sera requalifiée en rémunération.</li>
 *   <li>{@link #NON_CONFORME_DEPASSEMENT_PLAFOND} — montant supérieur
 *       au plafond légal (250 EUR/an pour les éco-chèques ; 6,91 EUR/
 *       jour ou 8 EUR facial pour les chèques-repas) : la fraction
 *       excédentaire est requalifiée en rémunération soumise aux
 *       cotisations ONSS et à l'impôt.</li>
 *   <li>{@link #NON_CONFORME_SUBSTITUTION_REMUNERATION} — l'avantage
 *       remplace une rémunération préexistante (interdit par l'art. 4
 *       CCT n°98 et la jurisprudence constante : Cass. 16/10/2017
 *       S.16.0042.F, ONSS c/ SA) : requalification intégrale en
 *       rémunération sur 5 ans rétroactifs (prescription ONSS).</li>
 *   <li>{@link #NON_CONFORME_CONDITION_MANQUANTE} — condition
 *       cumulative non remplie (chèques-repas : pas de jour
 *       effectivement presté, double avec indemnité forfaitaire frais
 *       de bouche, paiement non électronique post 01/01/2016 ;
 *       éco-chèques : absence de CCT sectorielle / d'entreprise ou
 *       de convention individuelle écrite, validité dépassée 2 ans,
 *       contribution travailleur insuffisante 1,09 EUR minimum) :
 *       requalification en rémunération.</li>
 *   <li>{@link #A_ANALYSER} — type d'avantage indéterminé ou
 *       configuration ambigüe nécessitant analyse cas par cas.</li>
 * </ul>
 */
public enum EcoChequesChequesRepasBeVerdict {
    /** Avantage intégralement conforme — exonération ONSS et impôt. */
    CONFORME_EXONERATION_INTEGRALE,

    /** Avantage partiellement conforme — fraction excédentaire à requalifier. */
    CONFORME_PARTIELLEMENT_EXONERE,

    /** Dépassement du plafond légal — requalification partielle. */
    NON_CONFORME_DEPASSEMENT_PLAFOND,

    /** Substitution à une rémunération — requalification intégrale rétroactive. */
    NON_CONFORME_SUBSTITUTION_REMUNERATION,

    /** Condition cumulative manquante — requalification. */
    NON_CONFORME_CONDITION_MANQUANTE,

    /** Configuration ambigüe — analyse cas par cas. */
    A_ANALYSER
}
