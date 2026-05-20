package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-217-12 : réponse de l'endpoint d'analyse d'option successorale BE.
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (pour pré-remplissage /
 * ré-édition du formulaire — leçon F-DT-36) ET les sorties calculées (verdict,
 * option recommandée, délai, risques, actions concrètes, bases juridiques).</p>
 */
public record SuccessionBeAcceptationRenonciationResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition UI) ---
        LocalDate dateDeces,
        SuccessionBeAcceptationRenonciationCalculator.QualiteHeritierBe qualiteHeritier,
        SuccessionBeAcceptationRenonciationCalculator.EtatPatrimoineSuccessoralBe etatPatrimoineSuccessoral,
        List<SuccessionBeAcceptationRenonciationCalculator.ActeHeritierBe> actesAccomplis,
        SuccessionBeAcceptationRenonciationCalculator.VolonteClientSuccessionBe volonteClient,
        Boolean miseEnDemeureCreancier,
        LocalDate dateMiseEnDemeureCreancier,
        String commentaire,
        // --- Outputs calculés ---
        SuccessionBeAcceptationRenonciationCalculator.SuccessionBeAcceptationRenonciationVerdict verdict,
        SuccessionBeAcceptationRenonciationCalculator.OptionRecommandeeSuccessionBe optionRecommandee,
        LocalDate dateLimiteOption,
        Integer joursRestants,
        SuccessionBeAcceptationRenonciationCalculator.DelaiStatutBe delaiStatut,
        List<String> actionsConcretes,
        List<SuccessionBeAcceptationRenonciationCalculator.RisqueSuccessionBe> risques,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
