package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-223-05 : requête HTTP pour les endpoints du corridor algérien BE.
 *
 * <p>Le pays cible est dérivé du workspace côté service — pas transmis dans le
 * body. {@code natureActe} et {@code lienRattachementBelgique} sont requis
 * (validation portée par le Calculator → 400 via
 * {@code IllegalArgumentException}).</p>
 */
public record RegimeAlgerienBeRequest(
        RegimeAlgerienBeCalculator.NatureActe natureActe,
        LocalDate dateActe,
        Boolean consentementEpouxEpouse,
        Boolean dotMahrPrevue,
        Double montantDotConnu,
        Boolean conventionAlgeroBelgeInvoquee,
        RegimeAlgerienBeCalculator.LienRattachement lienRattachementBelgique
) {

    RegimeAlgerienBeInput toInput() {
        return new RegimeAlgerienBeInput(
                natureActe,
                dateActe,
                consentementEpouxEpouse,
                dotMahrPrevue,
                montantDotConnu,
                conventionAlgeroBelgeInvoquee,
                lienRattachementBelgique);
    }
}
