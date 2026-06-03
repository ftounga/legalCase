package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-223-07 : requête HTTP pour les endpoints DIP loi applicable famille BE.
 *
 * <p>Le pays cible est dérivé du workspace côté service — pas transmis dans le
 * body. {@code matiere} est requise (validation portée par le Calculator → 400
 * via {@code IllegalArgumentException}).</p>
 */
public record DipBeLoiApplicableFamilleRequest(
        DipBeLoiApplicableFamilleCalculator.Matiere matiere,
        String residenceHabituelleCommune,
        String nationaliteCommune,
        String choixLoiParLesParties,
        LocalDate dateMariageOuDeces,
        String lieuSituationBiensImmobiliers
) {

    DipBeLoiApplicableFamilleInput toInput() {
        return new DipBeLoiApplicableFamilleInput(
                matiere,
                residenceHabituelleCommune,
                nationaliteCommune,
                choixLoiParLesParties,
                dateMariageOuDeces,
                lieuSituationBiensImmobiliers);
    }
}
