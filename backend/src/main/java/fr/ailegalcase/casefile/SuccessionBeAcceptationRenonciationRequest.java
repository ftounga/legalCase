package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-217-12 : requête HTTP pour les endpoints d'analyse d'option successorale BE.
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.
 * Tous les champs sont obligatoires sauf {@code dateMiseEnDemeureCreancier} (qui
 * devient obligatoire si {@code miseEnDemeureCreancier = true}) et
 * {@code commentaire}.</p>
 */
public record SuccessionBeAcceptationRenonciationRequest(
        LocalDate dateDeces,
        SuccessionBeAcceptationRenonciationCalculator.QualiteHeritierBe qualiteHeritier,
        SuccessionBeAcceptationRenonciationCalculator.EtatPatrimoineSuccessoralBe etatPatrimoineSuccessoral,
        List<SuccessionBeAcceptationRenonciationCalculator.ActeHeritierBe> actesAccomplis,
        SuccessionBeAcceptationRenonciationCalculator.VolonteClientSuccessionBe volonteClient,
        Boolean miseEnDemeureCreancier,
        LocalDate dateMiseEnDemeureCreancier,
        String commentaire
) {

    SuccessionBeAcceptationRenonciationInput toInput() {
        return new SuccessionBeAcceptationRenonciationInput(
                dateDeces,
                qualiteHeritier,
                etatPatrimoineSuccessoral,
                actesAccomplis == null ? List.of() : actesAccomplis,
                volonteClient,
                Boolean.TRUE.equals(miseEnDemeureCreancier),
                dateMiseEnDemeureCreancier,
                commentaire
        );
    }
}
