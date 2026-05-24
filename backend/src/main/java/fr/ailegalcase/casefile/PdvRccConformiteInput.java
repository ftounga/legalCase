package fr.ailegalcase.casefile;

/**
 * SF-212-35 : données d'entrée du calculateur d'analyse de la conformité d'un
 * plan de départs volontaires (PDV) ou d'une rupture conventionnelle
 * collective (RCC) (F-DT-46-pdv-rcc-conformite, FRANCE — fondements :
 * L. 1237-17 à L. 1237-19-14 CT — RCC instituée par l'ord. n°2017-1387 du
 * 22/09/2017 ; décret 2017-1718 du 20/12/2017 ; circulaire DGT du 19/09/2018 ;
 * L. 1237-19-1 al. 5 CT — indemnités au moins égales à l'indemnité légale de
 * licenciement ; L. 1237-19-3 CT — validation DREETS dans 15 j).
 *
 * <p>La RCC ouvre droit à l'allocation d'aide au retour à l'emploi (ARE)
 * pour les salariés volontaires (à la différence d'un PDV classique hors
 * PSE), à condition que l'accord soit majoritaire et validé par la DREETS.</p>
 *
 * @param typeDispositif                       type de dispositif (RCC ou PDV)
 * @param accordCollectifMajoritaire           accord signé par syndicats
 *                                              représentant ≥ 50 % suffrages
 *                                              exprimés au 1er tour
 *                                              (L. 1237-19-1 CT) — condition
 *                                              de validité d'une RCC
 * @param validationDREETSObtenue              la décision de validation
 *                                              DREETS a été obtenue
 * @param delaiValidation15JRespectee          la DREETS s'est prononcée dans
 *                                              le délai de 15 jours suivant
 *                                              dépôt de l'accord
 *                                              (L. 1237-19-3 CT) — null si
 *                                              non applicable
 * @param indemnitesAuMoinsLegales             les indemnités prévues par le
 *                                              dispositif sont au moins
 *                                              égales à l'indemnité légale
 *                                              de licenciement (plancher
 *                                              L. 1237-19-1 al. 5 CT)
 * @param adhesionVolontaireIndividuelle       l'adhésion individuelle du
 *                                              salarié est libre, écrite,
 *                                              dans un délai de réflexion
 *                                              (volet individuel L. 1237-19-2)
 * @param informationIndividuelleComplete      information individuelle
 *                                              complète du salarié sur ses
 *                                              droits (indemnités,
 *                                              accompagnement, droit ARE,
 *                                              modalités de rétractation)
 */
public record PdvRccConformiteInput(
        TypeDispositif typeDispositif,
        boolean accordCollectifMajoritaire,
        boolean validationDREETSObtenue,
        Boolean delaiValidation15JRespectee,
        boolean indemnitesAuMoinsLegales,
        boolean adhesionVolontaireIndividuelle,
        boolean informationIndividuelleComplete
) {

    /** Type de dispositif — RCC (accord collectif majoritaire + DREETS) ou PDV (hors RCC). */
    public enum TypeDispositif {
        RCC,
        PDV
    }
}
