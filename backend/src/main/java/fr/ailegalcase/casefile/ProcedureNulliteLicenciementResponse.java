package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-36-01 : réponse de l'endpoint d'analyse des nullités de procédure de
 * licenciement.
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI) ET les sorties
 * calculées (verdict, score, vices détectés, bases juridiques).</p>
 */
public record ProcedureNulliteLicenciementResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        Boolean convocationEnvoyee,
        LocalDate dateConvocationPresentee,
        LocalDate dateEntretienPrealable,
        Boolean entretienTenu,
        LocalDate dateNotificationLicenciement,
        Boolean lettreLicenciementEcrite,
        Boolean lettreMotivee,
        Boolean motivationSuffisante,
        String motivationCommentaire,
        Boolean licenciementPourMotifGrave,
        Boolean licenciementCollectif,
        Boolean procedureCseRespectee,
        Boolean conventionCollectiveApplicable,
        Boolean conventionCollectiveRespectee,
        String conventionCollectiveCommentaire,
        // --- Outputs calculés ---
        ProcedureNulliteLicenciementCalculator.Verdict verdict,
        int scoreNullite,
        List<ProcedureNulliteLicenciementCalculator.ViceDetecte> vicesDetectes,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
