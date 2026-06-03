package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-223-02 : réponse des endpoints POST / GET de recevabilité de l'adoption BE.
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (pour pré-remplissage /
 * ré-édition du formulaire — leçon F-DT-36) ET les sorties calculées (verdict,
 * motifs/conditions, actes à produire, bases juridiques, messages).</p>
 */
public record AdoptionBeResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition UI) ---
        AdoptionBeCalculator.TypeAdoptionBe typeAdoption,
        Integer ageAdoptant,
        Integer ageAdopte,
        Integer ecartAgeAnnees,
        Boolean adoptantEnCoupleAvecParentBiologique,
        Boolean lienCohabitationLegaleOuMariage,
        Boolean agrementObtenu,
        Boolean consentementAdopteOuRepresentants,
        AdoptionBeCalculator.ConsentementParentsBiologiques consentementParentsBiologiques,
        String commentaire,
        // --- Outputs calculés ---
        AdoptionBeCalculator.AdoptionBeVerdict verdict,
        List<String> motifsConditions,
        List<String> actesAProduire,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
