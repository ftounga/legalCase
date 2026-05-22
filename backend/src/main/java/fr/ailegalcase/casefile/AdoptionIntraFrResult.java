package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-15 : résultat du calcul Adoption intra-familiale FR
 * (art. 343-1 al. 2 + 345-1 + 360-362 Cciv).
 *
 * <ul>
 *   <li>{@code formesPossibles} — formes d'adoption ouvertes au demandeur
 *       (PLENIERE et/ou SIMPLE).</li>
 *   <li>{@code formeDemandee} — forme demandée par l'avocat (reproduite ici
 *       pour le snapshot UI).</li>
 *   <li>{@code conditionsRemplies} — true si l'ensemble des conditions
 *       communes de l'art. 345-1 al. 1 est satisfait pour la forme
 *       demandée.</li>
 *   <li>{@code alerteIrreversibilite} — true si la forme demandée est PLENIERE
 *       (rupture définitive de la filiation antérieure, art. 356 Cciv).</li>
 *   <li>{@code consentementEnfantRequis} — true si l'adopté a 13 ans ou plus
 *       (art. 360 al. 2).</li>
 *   <li>{@code baseLegale} — articles Cciv applicables.</li>
 *   <li>{@code messages} — informations contextuelles.</li>
 *   <li>{@code alertes} — points d'attention bloquants ou de vigilance.</li>
 * </ul>
 */
public record AdoptionIntraFrResult(
        List<FormeAdoptionEnum> formesPossibles,
        FormeAdoptionEnum formeDemandee,
        boolean conditionsRemplies,
        boolean alerteIrreversibilite,
        boolean consentementEnfantRequis,
        String baseLegale,
        List<String> messages,
        List<String> alertes
) {}
