package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-217-01 : réponse de l'endpoint d'analyse du régime de communauté légale belge.
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (date de mariage, contrat,
 * biens et dettes saisis) pour permettre la ré-édition du formulaire (leçon
 * F-DT-36), <b>+</b> les champs calculés (verdict, biens et dettes qualifiés,
 * synthèse, bases juridiques).</p>
 */
public record RegimeCommunauteLegaleBeResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition du formulaire) ---
        LocalDate dateMariage,
        Boolean contratMariageSigne,
        List<RegimeCommunauteLegaleBeRequest.BienRequest> biens,
        List<RegimeCommunauteLegaleBeRequest.DetteRequest> dettes,
        // --- Outputs calculés ---
        RegimeCommunauteLegaleBeVerdict verdict,
        List<RegimeCommunauteLegaleBeResult.BienQualifie> biensQualifies,
        List<RegimeCommunauteLegaleBeResult.DetteQualifiee> dettesQualifiees,
        RegimeCommunauteLegaleBeResult.SyntheseComposition syntheseComposition,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
