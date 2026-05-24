package fr.ailegalcase.casefile;

/**
 * SF-212-15 : requête HTTP pour l'endpoint d'analyse de la conformité du
 * dispositif de télétravail et des litiges courants
 * (F-DT-82-teletravail-accord, FRANCE — L. 1222-9 à L. 1222-11 CT ; ANI
 * télétravail 26/11/2020).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record TeletravailAccordRequest(
        TeletravailAccordInput.CadreTeletravail cadreTeletravail,
        boolean doubleVolontariatRespectee,
        boolean indemniteOccupationVersee,
        Double montantIndemniteJournalierEuros,
        boolean accidentDomicileDetecte,
        boolean retourBureauImposeUnilateralement,
        boolean refusTeletravailCauseIncrimination
) {

    TeletravailAccordInput toInput() {
        return new TeletravailAccordInput(
                cadreTeletravail,
                doubleVolontariatRespectee,
                indemniteOccupationVersee,
                montantIndemniteJournalierEuros,
                accidentDomicileDetecte,
                retourBureauImposeUnilateralement,
                refusTeletravailCauseIncrimination
        );
    }
}
