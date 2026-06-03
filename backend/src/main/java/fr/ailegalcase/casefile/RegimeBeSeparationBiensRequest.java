package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-223-06 : requête HTTP pour les endpoints du régime de séparation de biens
 * BE.
 *
 * <p>Le pays cible est dérivé du workspace côté service — pas transmis dans le
 * body. {@code varianteRegime} est requise (validation portée par le Calculator
 * → 400 via {@code IllegalArgumentException}).</p>
 */
public record RegimeBeSeparationBiensRequest(
        RegimeBeSeparationBiensCalculator.VarianteRegime varianteRegime,
        Boolean contratMariageNotarie,
        Boolean clauseParticipationPrevue,
        Boolean disproportionPatrimonialeAllegee,
        LocalDate dateContrat,
        Double patrimoinePropreEpoux1Eur,
        Double patrimoinePropreEpoux2Eur
) {

    RegimeBeSeparationBiensInput toInput() {
        return new RegimeBeSeparationBiensInput(
                varianteRegime,
                contratMariageNotarie,
                clauseParticipationPrevue,
                disproportionPatrimonialeAllegee,
                dateContrat,
                patrimoinePropreEpoux1Eur,
                patrimoinePropreEpoux2Eur);
    }
}
