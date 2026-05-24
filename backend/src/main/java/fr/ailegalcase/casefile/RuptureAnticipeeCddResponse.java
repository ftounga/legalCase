package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-17 : réponse de l'endpoint d'analyse de la rupture anticipée d'un
 * CDD (F-DT-43-rupture-anticipee-cdd, FRANCE — L. 1243-1 à L. 1243-4 CT ;
 * L. 1243-8 CT ; Cass. soc. constante).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI lors du
 * GET) ET les sorties calculées (verdict, mois restants, points d'analyse,
 * indemnités dues au salarié, bases juridiques).</p>
 */
public record RuptureAnticipeeCddResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        RuptureAnticipeeCddInput.AuteurRupture auteurRupture,
        RuptureAnticipeeCddInput.MotifRupture motifRupture,
        LocalDate dateTermeCdd,
        LocalDate dateRupture,
        double salaireMensuelBrutEuros,
        double remunerationBruteTotaleContratEuros,
        // --- Outputs calculés ---
        RuptureAnticipeeCddCalculator.AnalyseRuptureAnticipeeCdd analyseRuptureAnticipee,
        int moisRestantsContrat,
        List<RuptureAnticipeeCddCalculator.PointAnalyse> pointsAnalyse,
        double indemnitesRuptureAnticipeeEuros,
        double indemnitePrecariteEuros,
        double totalDuSalarieEuros,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
