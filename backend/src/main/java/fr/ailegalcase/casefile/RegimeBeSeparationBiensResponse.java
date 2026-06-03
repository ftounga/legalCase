package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-223-06 : réponse des endpoints POST / GET du régime de séparation de biens
 * BE.
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (pour pré-remplissage /
 * ré-édition du formulaire) ET les sorties calculées (verdict, motifs, effets
 * patrimoniaux, bases juridiques, messages).</p>
 */
public record RegimeBeSeparationBiensResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition UI) ---
        RegimeBeSeparationBiensCalculator.VarianteRegime varianteRegime,
        Boolean contratMariageNotarie,
        Boolean clauseParticipationPrevue,
        Boolean disproportionPatrimonialeAllegee,
        LocalDate dateContrat,
        Double patrimoinePropreEpoux1Eur,
        Double patrimoinePropreEpoux2Eur,
        // --- Outputs calculés ---
        RegimeBeSeparationBiensCalculator.RegimeBeSeparationBiensVerdict verdict,
        List<String> motifs,
        String effetsPatrimoniaux,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
