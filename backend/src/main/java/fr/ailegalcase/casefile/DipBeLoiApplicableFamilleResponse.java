package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-223-07 : réponse des endpoints POST / GET de l'outil DIP loi applicable
 * famille BE.
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (pour pré-remplissage /
 * ré-édition du formulaire) ET les sorties calculées (verdict, loi applicable,
 * fondement de rattachement, motifs, conseils, bases juridiques, messages).</p>
 */
public record DipBeLoiApplicableFamilleResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition UI) ---
        DipBeLoiApplicableFamilleCalculator.Matiere matiere,
        String residenceHabituelleCommune,
        String nationaliteCommune,
        String choixLoiParLesParties,
        LocalDate dateMariageOuDeces,
        String lieuSituationBiensImmobiliers,
        // --- Outputs calculés ---
        DipBeLoiApplicableFamilleCalculator.DipBeLoiApplicableFamilleVerdict verdict,
        String loiApplicableDeterminee,
        DipBeLoiApplicableFamilleCalculator.FondementRattachement fondementRattachement,
        List<String> motifs,
        List<String> conseils,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
