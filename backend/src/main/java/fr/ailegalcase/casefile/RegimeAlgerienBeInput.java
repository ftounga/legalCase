package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-223-05 : input du moteur décisionnel BE du corridor algérien
 * (reconnaissance mariage / talaq / dot relevant du droit algérien).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.
 * {@code natureActe} et {@code lienRattachementBelgique} sont requis (validation
 * portée par le Calculator → 400). Les autres champs sont nullables
 * (informatifs / pré-remplissables).</p>
 */
public record RegimeAlgerienBeInput(
        RegimeAlgerienBeCalculator.NatureActe natureActe,
        LocalDate dateActe,
        Boolean consentementEpouxEpouse,
        Boolean dotMahrPrevue,
        Double montantDotConnu,
        Boolean conventionAlgeroBelgeInvoquee,
        RegimeAlgerienBeCalculator.LienRattachement lienRattachementBelgique
) {
    public RegimeAlgerienBeInput {
        conventionAlgeroBelgeInvoquee = Boolean.TRUE.equals(conventionAlgeroBelgeInvoquee);
    }
}
