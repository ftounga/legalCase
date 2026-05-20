package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-206-01 : réponse de l'endpoint d'analyse de contestation d'une présomption
 * de démission par abandon de poste (FRANCE).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI) ET les
 * sorties calculées (verdict, score, motifs détectés, dateExpirationDelai,
 * bases juridiques).</p>
 */
public record AbandonPostePresomptionDemissionResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        LocalDate dateMiseEnDemeurePresentee,
        AbandonPostePresomptionDemissionCalculator.ModeNotification modeNotificationMiseEnDemeure,
        Integer delaiAccordeJours,
        Boolean miseEnDemeureMentionneDelai,
        Boolean miseEnDemeureMentionneConsequences,
        AbandonPostePresomptionDemissionCalculator.MotifAbsence motifAbsenceInvoque,
        String motifAbsenceCommentaire,
        Boolean repriseOuJustificationDansDelai,
        LocalDate dateRepriseOuJustification,
        // --- Outputs calculés ---
        AbandonPostePresomptionDemissionCalculator.Verdict verdict,
        int scoreContestation,
        List<AbandonPostePresomptionDemissionCalculator.MotifContestation> motifsContestation,
        LocalDate dateExpirationDelai,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
