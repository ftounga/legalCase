package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-25 : réponse de l'endpoint d'évaluation de la protection du
 * lanceur d'alerte (F-DT-61, FRANCE — L. 1132-3-3 CT ; loi Sapin II ;
 * loi Waserman 2022).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI lors
 * du GET) ET les sorties calculées (verdict, score, points d'analyse
 * F-IA-03, nullité mesure, montant minimum DI Waserman, bases juridiques,
 * messages).</p>
 */
public record LanceurAlerteProtectionResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        LanceurAlerteProtectionInput.NatureSignalement natureSignalement,
        Boolean contrepartieFinanciere,
        LanceurAlerteProtectionInput.ProcedureSignalement procedureSignalement,
        Boolean referentInterneSaisi,
        Boolean mesureRepresailleDetectee,
        LanceurAlerteProtectionInput.NatureMesureRepresaille natureMesureRepresaille,
        // --- Outputs calculés ---
        LanceurAlerteProtectionCalculator.AnalyseLanceurAlerte analyseLanceurAlerte,
        int scoreProtection,
        List<LanceurAlerteProtectionCalculator.PointAnalyse> pointsAnalyse,
        boolean nulliteMesureRepresaille,
        double montantMinDommagesInterets,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
