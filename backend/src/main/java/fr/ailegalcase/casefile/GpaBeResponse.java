package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-223-04 : réponse des endpoints POST / GET de la situation contentieuse
 * post-GPA BE.
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (pour pré-remplissage /
 * ré-édition du formulaire) ET les sorties calculées (verdict, chemin
 * contentieux, risques, bases juridiques, messages).</p>
 */
public record GpaBeResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition UI) ---
        GpaBeCalculator.LieuGpa gpaRealiseeEnBelgiqueOuEtranger,
        GpaBeCalculator.LienGenetique lienGenetiqueParentIntentionnel,
        Boolean acteNaissanceEtrangerEtabli,
        Boolean merePorteuseDesignee,
        Boolean consentementMerePorteuse,
        Boolean coupleIntentionnelMarieOuCohabitant,
        String commentaire,
        // --- Outputs calculés ---
        GpaBeCalculator.GpaBeVerdict verdict,
        List<String> cheminContentieux,
        List<String> risques,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
