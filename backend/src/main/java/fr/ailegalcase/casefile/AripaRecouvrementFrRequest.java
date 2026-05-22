package fr.ailegalcase.casefile;

/**
 * SF-216-07 : body POST /api/v1/case-files/{id}/aripa-recouvrement-fr.
 *
 * <p>Outil single-country FRANCE — art. L. 581-1 à L. 581-14 CSS + art. L. 582-1
 * CSS + art. L. 523-1 à L. 523-6 CSS + Ordonnance n°2002-1358 + Convention La
 * Haye 2007 + Règlement UE n°4/2009.</p>
 *
 * <p>Tous les montants en euros entiers (Integer). Le service rejette les
 * valeurs négatives ou les durées d'impayé inférieures à 1 mois en 400.</p>
 */
public record AripaRecouvrementFrRequest(
        Integer montantPensionMensuelleEur,
        Integer nombreMoisImpayes,
        SituationAripaEnum situationCreancier,
        SituationAripaEnum situationDebiteur,
        Boolean titreExecutoire,
        Boolean debiteurEnFrance,
        Boolean debiteurEmployeurPublic,
        Integer nombreEnfantsACharge
) {}
