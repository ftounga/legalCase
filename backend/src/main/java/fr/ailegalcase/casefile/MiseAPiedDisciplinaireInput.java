package fr.ailegalcase.casefile;

/**
 * SF-212-19 : données d'entrée du calculateur d'analyse de la régularité d'une
 * mise à pied disciplinaire (F-DT-48-mise-a-pied-disciplinaire, FRANCE —
 * fondements : L. 1331-1 CT ; L. 1332-1 à L. 1332-3 CT ; L. 1332-4 CT —
 * prescription des faits ; L. 1311-2 CT — durée définie dans le règlement
 * intérieur ; Cass. soc. 26/10/2010 n°09-42.740 — distinction mise à pied
 * conservatoire / disciplinaire ; jurisprudence constante Cass. soc. —
 * interdiction de la double sanction des mêmes faits).
 *
 * @param natureMiseAPied               nature de la mise à pied
 *                                      (DISCIPLINAIRE = sanction définitive
 *                                      privant de salaire, inscrite au dossier
 *                                      du salarié ; CONSERVATOIRE = mesure
 *                                      provisoire dans l'attente de la
 *                                      décision finale, normalement rémunérée
 *                                      si la sanction finale est inférieure
 *                                      au licenciement — Cass. soc.
 *                                      26/10/2010 ; INCONNUE = nature non
 *                                      qualifiée par l'employeur, à requalifier)
 * @param procedureEntretienSuivie      procédure d'entretien préalable
 *                                      (L. 1332-2 CT) effectivement suivie
 *                                      (convocation + entretien tenu + délai
 *                                      de notification)
 * @param prescriptionFauteVerifiee     prescription des faits respectée :
 *                                      la sanction a été notifiée dans le
 *                                      délai de 2 mois à compter de la
 *                                      connaissance des faits par l'employeur
 *                                      (L. 1332-4 CT)
 * @param dureeDefiniedansRI            durée de la mise à pied disciplinaire
 *                                      définie dans le règlement intérieur
 *                                      (L. 1311-2 CT) ou dans l'accord
 *                                      collectif applicable — condition de
 *                                      validité du quantum de la sanction
 * @param dureeJours                    durée de la mise à pied disciplinaire
 *                                      en nombre de jours calendaires
 * @param salaireSuspendu               salaire effectivement suspendu pendant
 *                                      la période de mise à pied (true pour
 *                                      une mise à pied disciplinaire régulière,
 *                                      irrégulier pour une mise à pied
 *                                      conservatoire suivie d'une sanction
 *                                      inférieure au licenciement)
 * @param sancionsAnterieuresMemesFaits une sanction antérieure a déjà été
 *                                      prononcée pour les mêmes faits —
 *                                      double sanction interdite par la
 *                                      jurisprudence (Cass. soc. constante)
 * @param salaireMensuelBrutEuros       salaire mensuel brut de référence en
 *                                      euros, base du calcul du salaire dû
 *                                      pendant la période (si la mise à pied
 *                                      est conservatoire ou irrégulière)
 */
public record MiseAPiedDisciplinaireInput(
        NatureMiseAPied natureMiseAPied,
        boolean procedureEntretienSuivie,
        boolean prescriptionFauteVerifiee,
        boolean dureeDefiniedansRI,
        Integer dureeJours,
        boolean salaireSuspendu,
        boolean sancionsAnterieuresMemesFaits,
        double salaireMensuelBrutEuros
) {

    /** Nature de la mise à pied — distinction Cass. soc. 26/10/2010 n°09-42.740. */
    public enum NatureMiseAPied {
        DISCIPLINAIRE,
        CONSERVATOIRE,
        INCONNUE
    }
}
