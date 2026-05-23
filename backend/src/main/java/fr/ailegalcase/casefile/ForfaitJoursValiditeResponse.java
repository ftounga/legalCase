package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-03 : réponse de l'endpoint d'analyse de validité du forfait jours
 * (FRANCE — L. 3121-58 à L. 3121-66 CT ; Cass. soc. 29/06/2011 n°09-71.107).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI) ET les
 * sorties calculées (verdict de validité, score, facteurs d'invalidité,
 * rappel HS estimé sur 3 ans, bases juridiques).</p>
 */
public record ForfaitJoursValiditeResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        Boolean accordCollectifExiste,
        Boolean accordGarantitSuiviCharge,
        Boolean entretienAnnuelRealise,
        Boolean documentControleMensuelExiste,
        Boolean categorieAutonomeConfirmee,
        int nbJoursForfait,
        int ancienneteMois,
        double salaireMensuelBrutEuros,
        int hsEstimeesParSemaine,
        int nbSemainesParAn,
        // --- Outputs calculés ---
        ForfaitJoursValiditeCalculator.ValiditeForfait validiteForfait,
        int scoreValidite,
        List<ForfaitJoursValiditeCalculator.FacteurInvalidite> facteursInvalidite,
        Double rappelHsEstimeEuros,
        int prescriptionRappelAns,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
