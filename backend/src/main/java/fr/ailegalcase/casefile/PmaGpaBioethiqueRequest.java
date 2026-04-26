package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-FA-27-01 : requête pour l'analyse des dispositifs bioéthique familiale (loi 2/8/2021).
 *
 * <p>3 dispositifs distincts :
 * <ul>
 *   <li><b>PMA_RECONNAISSANCE_ANTICIPEE</b> — art. 342-9 et s. Cciv :
 *       reconnaissance conjointe notariée AVANT la PMA (couples de femmes / femme seule).</li>
 *   <li><b>GPA_TRANSCRIPTION_ETAT_CIVIL</b> — Cass. 4 arrêts 18/12/2022 :
 *       transcription parent biologique automatique, autre parent par adoption simple.</li>
 *   <li><b>DON_GAMETES_ACCES_ORIGINES</b> — art. 16-8-1 Cciv (depuis 1/9/2022) :
 *       accès à 18 ans à l'identité du donneur.</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>. L'équivalent belge (loi 6/7/2007 PMA BE,
 * GPA non encadrée explicitement) sera traité dans une feature jumelle au backlog (F-FA-27-BE).
 *
 * @param dispositif                       code dispositif (3 valeurs)
 * @param consentementsConjointsNotaire    PMA : consentement conjoint reçu par notaire
 * @param dateReconnaissanceAnterieurePMA  PMA : date de la reconnaissance (doit précéder PMA)
 * @param datePMA                          PMA : date de la procédure médicale (comparée)
 * @param conditionsAccesPMA               PMA : couple/femme respecte conditions légales
 * @param paysGPALegal                     GPA : pays étranger où la GPA est légale
 * @param parentBiologiqueAvere            GPA : un des parents d'intention est biologique
 * @param decisionEtrangereProduite        GPA : décision étrangère établissant la filiation
 * @param adoptionDemande                  GPA : adoption simple demandée pour l'autre parent
 * @param dateDon                          ACCES : date du don (doit être ≥ 1/9/2022 pour ID)
 * @param demandeAcces                     ACCES : demande formelle d'accès aux origines
 * @param ageDemandeur                     ACCES : âge du demandeur (doit être ≥ 18)
 */
public record PmaGpaBioethiqueRequest(
        String dispositif,
        Boolean consentementsConjointsNotaire,
        LocalDate dateReconnaissanceAnterieurePMA,
        LocalDate datePMA,
        Boolean conditionsAccesPMA,
        Boolean paysGPALegal,
        Boolean parentBiologiqueAvere,
        Boolean decisionEtrangereProduite,
        Boolean adoptionDemande,
        LocalDate dateDon,
        Boolean demandeAcces,
        Integer ageDemandeur
) {}
