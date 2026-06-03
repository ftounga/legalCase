package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-223-01 : réponse des endpoints POST / GET de la cohabitation légale BE.
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (pour pré-remplissage /
 * ré-édition du formulaire — leçon F-DT-36) ET les sorties calculées (verdict,
 * conditions, actes à produire, bases juridiques, messages).</p>
 */
public record CohabitationLegaleBeResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition UI) ---
        CohabitationLegaleBeCalculator.VueCohabitationLegaleBe vue,
        Boolean deuxPersonnesNonMariees,
        Boolean capaciteJuridique,
        Boolean pasDejaLieParMariageOuAutreCohabitation,
        Boolean domicileCommun,
        Boolean logementFamilialEnJeu,
        CohabitationLegaleBeCalculator.ModeDissolutionCohabitationLegaleBe modeDissolutionEnvisage,
        String commentaire,
        // --- Outputs calculés ---
        CohabitationLegaleBeCalculator.CohabitationLegaleBeVerdict verdict,
        List<String> conditions,
        List<String> actesAProduire,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
