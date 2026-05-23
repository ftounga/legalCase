package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-07 : réponse de l'endpoint de conformité CSP/CRP
 * (FRANCE — L. 1233-65 à L. 1233-70 CT ; ANI CSP 19/07/2011 ; DARES).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI) ET les
 * sorties calculées (verdict de conformité, score, points de non-conformité,
 * ASP estimée journalière + annuelle, durée 12 mois, bases juridiques).</p>
 */
public record CspCrpConformiteResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        int effectifEntreprise,
        Boolean cspPropose,
        Boolean documentInformationRemis,
        Boolean delaiReflexionMentionne,
        LocalDate dateRemise,
        LocalDate dateEntretienPrealable,
        Boolean adhesionSalarie,
        double salaireMensuelBrutEuros,
        double remunerationBrute12MoisEuros,
        // --- Outputs calculés ---
        boolean obligationCspApplicable,
        CspCrpConformiteCalculator.ConformiteCsp conformiteCsp,
        int scoreConformite,
        List<CspCrpConformiteCalculator.PointNonConformite> pointsNonConformite,
        Double aspEstimeeJournaliereEuros,
        Double aspEstimeeAnnuelleEuros,
        int dureeAspMois,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
