package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-09 : résultat du calcul de recevabilité d'une délégation d'autorité
 * parentale (art. 376-1 Cciv).
 *
 * <ul>
 *   <li>{@code verdictRecevabilite} — recevabilité de la délégation envisagée.</li>
 *   <li>{@code voieProcedurale} — voie retenue effectivement (peut différer
 *       du type demandé en cas de bascule, ex. volontaire sans accord →
 *       judiciaire tiers).</li>
 *   <li>{@code etapes} — étapes concrètes à mener par l'avocat.</li>
 *   <li>{@code dureeEstimeeJours} — durée indicative de la procédure (jours).</li>
 *   <li>{@code baseLegale} — articles applicables.</li>
 *   <li>{@code messages} — informations contextuelles (durée AP, révocation).</li>
 *   <li>{@code alertes} — points de vigilance (danger caractérisé → retrait AP,
 *       enfant en bas âge, etc.).</li>
 * </ul>
 */
public record DelegationApFrResult(
        VerdictRecevabiliteDelegationApEnum verdictRecevabilite,
        TypeDelegationApEnum voieProcedurale,
        List<String> etapes,
        int dureeEstimeeJours,
        String baseLegale,
        List<String> messages,
        List<String> alertes
) {}
