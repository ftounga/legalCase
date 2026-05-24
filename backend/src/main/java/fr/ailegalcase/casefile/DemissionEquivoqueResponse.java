package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-21 : réponse de l'endpoint d'analyse de la validité d'une démission
 * (F-DT-41-demission-validite-equivoque, FRANCE — L. 1237-1 CT ; Cass. soc.
 * 09/05/2007 sur la volonté claire et non équivoque).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI lors du
 * GET) ET les sorties calculées (verdict 3 niveaux, score équivocité,
 * facteurs F-IA-03, bases juridiques, indicateur de requalification possible).</p>
 */
public record DemissionEquivoqueResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        DemissionEquivoqueInput.ModeExpressionDemission modeExpressionDemission,
        boolean contexteAltercation,
        boolean pressionOuMenace,
        boolean retractationDansDelai,
        Integer delaiRetractationJours,
        boolean manquementsEmployeurContemporains,
        boolean etatEmotionnelPerturbe,
        // --- Outputs calculés ---
        DemissionEquivoqueCalculator.AnalyseDemission analyseDemission,
        int scoreEquivocite,
        List<DemissionEquivoqueCalculator.FacteurEquivocite> facteursEquivocite,
        boolean requalificationPossible,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
