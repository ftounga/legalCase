package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-223-03 : réponse des endpoints POST / GET du recueil légal (kafala) BE.
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (pour pré-remplissage /
 * ré-édition du formulaire — leçon F-DT-36) ET les sorties calculées (verdict,
 * motifs/conditions, actes à produire, bases juridiques, messages).</p>
 */
public record KafalaBeResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition UI) ---
        String paysOrigineKafala,
        LocalDate dateActeKafala,
        Boolean acteOfficielJudiciaireOuAdoul,
        Boolean enfantAbandonneOuOrphelin,
        Boolean kafilDomicilieBelgique,
        Boolean consentementParentsBiologiquesOuAutorite,
        KafalaBeCalculator.ObjectifEnvisage objectifEnvisage,
        String commentaire,
        // --- Outputs calculés ---
        KafalaBeCalculator.KafalaBeVerdict verdict,
        List<String> motifsConditions,
        List<String> actesAProduire,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
