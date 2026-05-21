package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-217-18 : réponse des endpoints d'analyse de recevabilité d'une contestation
 * de filiation BE.
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (pour pré-remplissage /
 * ré-édition du formulaire — leçon F-DT-36) ET les sorties calculées (verdict,
 * délai, motifs d'irrecevabilité, voie procédurale, actions concrètes, bases
 * juridiques).</p>
 */
public record ContestationFiliationBeResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition UI) ---
        ContestationFiliationBeCalculator.NatureActionFiliationBe natureActionFiliation,
        ContestationFiliationBeCalculator.QualiteDemandeurFiliationBe qualiteDemandeur,
        LocalDate dateNaissanceEnfant,
        LocalDate dateConnaissanceFaitContestation,
        Boolean possessionEtatConforme,
        Integer dureePossessionEtatAnnees,
        Boolean expertiseAdnDisponible,
        Boolean demandeExpertiseAdnEnvisagee,
        String commentaire,
        // --- Outputs calculés ---
        ContestationFiliationBeCalculator.ContestationFiliationBeVerdict verdict,
        LocalDate dateLimiteAction,
        Integer joursRestants,
        ContestationFiliationBeCalculator.DelaiStatutBe delaiStatut,
        ContestationFiliationBeCalculator.VoieProceduraleFiliationBe voieProcedurale,
        List<ContestationFiliationBeCalculator.MotifIrrecevabiliteFiliationBe> motifsIrrecevabilite,
        List<String> actionsConcretes,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
