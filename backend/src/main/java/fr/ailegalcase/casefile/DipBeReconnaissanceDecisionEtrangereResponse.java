package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-223-08 : réponse des endpoints POST / GET de l'outil DIP reconnaissance /
 * exequatur d'une décision familiale étrangère BE.
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (pour pré-remplissage /
 * ré-édition du formulaire) ET les sorties calculées (verdict, motifs, conseils,
 * actes à produire, bases juridiques, messages).</p>
 */
public record DipBeReconnaissanceDecisionEtrangereResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition UI) ---
        DipBeReconnaissanceDecisionEtrangereCalculator.NatureDecision natureDecision,
        String paysOrigine,
        LocalDate dateDecision,
        Boolean decisionDefinitive,
        Boolean droitsDefenseRespectes,
        boolean conformiteOrdrePublicBelge,
        Boolean absenceFraude,
        Boolean mariageCivilPrealable,
        // --- Outputs calculés ---
        DipBeReconnaissanceDecisionEtrangereCalculator.DipBeReconnaissanceVerdict verdict,
        List<String> motifs,
        List<String> conseils,
        List<String> actesAProduire,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
