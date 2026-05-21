package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-217-14 : réponse de l'endpoint d'analyse de protection du majeur BE.
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (pour pré-remplissage /
 * ré-édition du formulaire — leçon F-DT-36) ET les sorties calculées (verdict,
 * mesure recommandée, juridiction, actes protégés, actions concrètes,
 * bases juridiques, messages).</p>
 */
public record ProtectionMajeurBeResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition UI) ---
        ProtectionMajeurBeCalculator.NatureAlterationBe natureAlteration,
        ProtectionMajeurBeCalculator.GraviteIncapaciteBe graviteIncapacite,
        Boolean mandatExtraJudiciaireSigne,
        LocalDate mandatExtraJudiciaireDateSignature,
        Boolean declarationAnticipeeExiste,
        Boolean environnementFamilialProtecteur,
        ProtectionMajeurBeCalculator.NiveauUrgenceProtectionBe niveauUrgence,
        ProtectionMajeurBeCalculator.ModeSaisineProtectionBe modeSaisineEnvisage,
        String commentaire,
        // --- Outputs calculés ---
        ProtectionMajeurBeCalculator.ProtectionMajeurBeVerdict verdict,
        ProtectionMajeurBeCalculator.MesureProtectionBe mesureRecommandee,
        ProtectionMajeurBeCalculator.JuridictionProtectionBe juridictionCompetente,
        String fondementProcedural,
        List<ProtectionMajeurBeCalculator.ActeProtegeBe> actesProteges,
        List<String> actionsConcretes,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
