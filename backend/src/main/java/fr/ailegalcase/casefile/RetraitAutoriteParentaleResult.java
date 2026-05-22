package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-11 : résultat du calcul de recevabilité d'un retrait d'autorité
 * parentale (art. 378-381 Cciv + loi 2022-140 LMVSS).
 *
 * <ul>
 *   <li>{@code verdictRetrait} — recevabilité et nature du retrait.</li>
 *   <li>{@code voieProcedurale} — voie procédurale effective (pénale,
 *       civile JAF, suspension LMVSS, etc.).</li>
 *   <li>{@code admissibiliteAdoption} — true si le retrait envisagé satisfait
 *       la condition d'admissibilité de l'enfant à l'adoption intra-familiale
 *       (art. 343-1 al. 2 Cciv).</li>
 *   <li>{@code consequencesJuridiques} — conséquences listées (délégation à
 *       tiers, ouverture tutelle, etc.).</li>
 *   <li>{@code etapes} — étapes concrètes à mener par l'avocat.</li>
 *   <li>{@code dureeEstimeeJours} — durée indicative de la procédure (jours).</li>
 *   <li>{@code baseLegale} — articles applicables.</li>
 *   <li>{@code messages} — informations contextuelles.</li>
 *   <li>{@code alertes} — points de vigilance (orientation procureur, etc.).</li>
 * </ul>
 */
public record RetraitAutoriteParentaleResult(
        VerdictRetraitApEnum verdictRetrait,
        VoieProceduraleRetraitApEnum voieProcedurale,
        boolean admissibiliteAdoption,
        List<String> consequencesJuridiques,
        List<String> etapes,
        int dureeEstimeeJours,
        String baseLegale,
        List<String> messages,
        List<String> alertes
) {}
