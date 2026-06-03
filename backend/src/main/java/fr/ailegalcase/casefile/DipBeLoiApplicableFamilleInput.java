package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-223-07 : input du moteur décisionnel BE déterminant la loi applicable en
 * droit de la famille (DIP : Rome III / Règl. 2016/1103 / Règl. 650/2012).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.
 * {@code matiere} est requise (validation portée par le Calculator → 400). Les
 * champs de rattachement sont des codes pays ISO 3166-1 alpha-2 nullables ;
 * {@code dateMariageOuDeces} est facultative.</p>
 */
public record DipBeLoiApplicableFamilleInput(
        DipBeLoiApplicableFamilleCalculator.Matiere matiere,
        String residenceHabituelleCommune,
        String nationaliteCommune,
        String choixLoiParLesParties,
        LocalDate dateMariageOuDeces,
        String lieuSituationBiensImmobiliers
) {}
