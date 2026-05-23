package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-05 : réponse de l'endpoint d'analyse de transfert d'entreprise
 * (FRANCE — L. 1224-1 CT).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI) ET les
 * sorties calculées (verdict applicabilité, score, points d'analyse,
 * alertes, bases juridiques).</p>
 */
public record TransfertEntrepriseL12241Response(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        TransfertEntrepriseL12241Calculator.TypeTransfert typeTransfert,
        Boolean eeaIdentifieeAvantTransfert,
        Boolean activiteEconomiquePreservee,
        Boolean salariesTransferes,
        Boolean contratsModifiesParRepreneur,
        Boolean licenciementsPreTransfert,
        int nbLicenciementsPreTransfert,
        Boolean informationConsultationCseRealisee,
        LocalDate dateTransfert,
        // --- Outputs calculés ---
        TransfertEntrepriseL12241Calculator.AnalyseTransfert analyseL1224_1,
        int scoreApplicabilite,
        List<TransfertEntrepriseL12241Calculator.PointAnalyseTransfert> pointsAnalyse,
        boolean alerteLicenciementsFrauduleux,
        boolean alerteDefautConsultationCse,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
