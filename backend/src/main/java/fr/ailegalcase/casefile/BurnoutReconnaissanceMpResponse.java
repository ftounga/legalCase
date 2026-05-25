package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-27 : réponse de l'endpoint d'évaluation des chances de
 * reconnaissance du burn-out comme maladie professionnelle hors tableau
 * (F-DT-64-burnout-reconnaissance-mp, FRANCE — L. 461-1 al. 4 et 5 CSS ;
 * CRRMP comité régional de reconnaissance des maladies professionnelles ;
 * circulaire DGT 2016/01).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI lors
 * du GET) ET les sorties calculées (verdict 3 niveaux, score, facteurs
 * d'analyse F-IA-03, conditions légales, délai d'instruction CRRMP,
 * bases juridiques, messages).</p>
 */
public record BurnoutReconnaissanceMpResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        Boolean diagnosticBurnoutPose,
        Double tauxIPPEstime,
        Integer anneesExpositionProfessionnelle,
        Boolean surchargeChargeDocumentee,
        Boolean manquementsSecuriteDocumentes,
        Boolean harcelementConcomitant,
        Boolean arretsMaladieMultiples,
        Boolean lienCausalDirectEtabli,
        // --- Outputs calculés ---
        BurnoutReconnaissanceMpCalculator.AnalyseBurnoutMp analyseChancesCRRMP,
        int scoreChances,
        boolean conditionIPPRemplie,
        boolean alerteIPPInsuffisante,
        int delaiInstructionMois,
        List<BurnoutReconnaissanceMpCalculator.FacteurDossier> facteursDossier,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
