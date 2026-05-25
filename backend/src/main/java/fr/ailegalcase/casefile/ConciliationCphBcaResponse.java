package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-37 : réponse de l'endpoint de préparation de la conciliation BCO
 * (F-DT-84-conciliation-cph-bca, FRANCE — R. 1454-7 à R. 1454-12 CT ;
 * L. 1235-1 al. 3 CT — barème transactions BCA).
 *
 * <p>Inclut un snapshot complet des inputs (pour ré-édition du formulaire UI
 * lors du GET) ET les sorties calculées (montant minimum BCA, palier
 * applicable, comparaison BCA vs Macron, checklist BCO).</p>
 */
public record ConciliationCphBcaResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        Integer ancienneteMois,
        Double salaireMensuelBrutEuros,
        Double montantDemandesEuros,
        ConciliationCphBcaInput.ConciliationCphBcaOpportunite opportuniteConciliation,
        // --- Outputs calculés ---
        double montantMinimumBcaEuros,
        String palierBcaApplicable,
        String comparaisonBcaVsMacron,
        List<String> checklistBco,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
