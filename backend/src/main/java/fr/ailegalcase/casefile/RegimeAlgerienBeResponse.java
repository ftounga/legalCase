package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-223-05 : réponse des endpoints POST / GET du corridor algérien BE.
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (pour pré-remplissage /
 * ré-édition du formulaire) ET les sorties calculées (verdict, motifs, effets de
 * la dot, bases juridiques, messages).</p>
 */
public record RegimeAlgerienBeResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition UI) ---
        RegimeAlgerienBeCalculator.NatureActe natureActe,
        LocalDate dateActe,
        Boolean consentementEpouxEpouse,
        Boolean dotMahrPrevue,
        Double montantDotConnu,
        Boolean conventionAlgeroBelgeInvoquee,
        RegimeAlgerienBeCalculator.LienRattachement lienRattachementBelgique,
        // --- Outputs calculés ---
        RegimeAlgerienBeCalculator.RegimeAlgerienBeVerdict verdict,
        List<String> motifs,
        String effetsDot,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
