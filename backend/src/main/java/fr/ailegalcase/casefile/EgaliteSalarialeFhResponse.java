package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-23 : réponse de l'endpoint d'analyse d'une discrimination salariale
 * fondée sur le sexe (F-DT-56-egalite-salariale-femmes-hommes, FRANCE —
 * L. 1142-7 à L. 1142-10 CT ; L. 1144-1 CT ; L. 3221-2 CT ; L. 1132-1 CT ;
 * L. 1134-5 CT ; loi 05/09/2018).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI lors du
 * GET) ET les sorties calculées (verdict, score, facteurs de disparité,
 * prescription, alerte non-plafonnement, bases juridiques).</p>
 */
public record EgaliteSalarialeFhResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        EgaliteSalarialeFhInput.SexeSalarie sexeSalarie,
        double salaireMensuelBrutSalarieEuros,
        int ancienneteMois,
        String qualification,
        int nombreComparantsMieuxPayes,
        double ecartSalaireMoyenComparantsEuros,
        double ecartPourcentage,
        boolean indexEgaliteConnu,
        Integer scoreIndexEgalite,
        boolean justificationsEmployeurObjectives,
        // --- Outputs calculés ---
        EgaliteSalarialeFhCalculator.AnalyseEgaliteSalariale analyseEgaliteSalariale,
        int scoreDiscrimination,
        List<EgaliteSalarialeFhCalculator.FacteurDisparite> facteursDisparite,
        int prescriptionActionAns,
        String alerteNonPlafonnement,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
