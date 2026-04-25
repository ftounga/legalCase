package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-DT-24-01 : requête HTTP pour l'endpoint d'analyse de validité d'une clause
 * de non-concurrence (FR).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record NonConcurrenceRequest(
        Boolean clausePresenteContrat,
        Boolean limiteTerritoireDefini,
        String territoireDescription,
        Boolean limiteDureeDefinie,
        Integer dureeMois,
        Boolean limiteObjetDefini,
        String objetDescription,
        Boolean contrepartieFinancierePresente,
        BigDecimal contrepartieMontantMensuelEur,
        BigDecimal salaireMensuelBrutEur,
        NonConcurrenceCalculator.SecteurActivite secteurActivite,
        LocalDate datePriseEffet
) {

    NonConcurrenceInput toInput() {
        return new NonConcurrenceInput(
                clausePresenteContrat,
                limiteTerritoireDefini,
                territoireDescription,
                limiteDureeDefinie,
                dureeMois,
                limiteObjetDefini,
                objetDescription,
                contrepartieFinancierePresente,
                contrepartieMontantMensuelEur,
                salaireMensuelBrutEur,
                secteurActivite,
                datePriseEffet
        );
    }
}
