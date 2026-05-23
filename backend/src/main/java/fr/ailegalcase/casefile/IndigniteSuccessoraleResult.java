package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-19 : résultat du calcul Indignité successorale FR (art. 726-729-1 Cciv).
 *
 * <ul>
 *   <li>{@code typeIndignite} — "PLEIN_DROIT" (art. 726), "JUDICIAIRE"
 *       (art. 727), "PARDONNEE" (neutralisée par testament art. 728), ou
 *       "AUCUNE" (motif ne caractérise pas l'indignité).</li>
 *   <li>{@code verdictIndignite} — code court résumant la conclusion
 *       ("INDIGNITE_PLEIN_DROIT", "INDIGNITE_JUDICIAIRE_POSSIBLE",
 *       "PARDON_NEUTRALISANT", "DELAI_FORCLOS", "CONDAMNATION_REQUISE",
 *       "PAS_D_INDIGNITE").</li>
 *   <li>{@code pardonNeutralisant} — true si le pardon testamentaire neutralise
 *       l'indignité (art. 728 Cciv).</li>
 *   <li>{@code representationPossible} — true si les descendants de l'indigne
 *       peuvent représenter leur parent (art. 729-1 — autorisée en cas
 *       d'indignité judiciaire, refusée pour indignité de plein droit).</li>
 *   <li>{@code delaiAction} — texte décrivant le délai applicable (art. 729 :
 *       5 ans à compter de la succession ou de la condamnation).</li>
 *   <li>{@code delaiForclos} — true si le délai 5 ans est écoulé.</li>
 *   <li>{@code effetDevolution} — texte décrivant l'effet de l'indignité sur
 *       la dévolution (l'indigne est écarté ; sa part est dévolue aux autres
 *       héritiers ou à ses descendants par représentation si possible).</li>
 *   <li>{@code baseLegale} — articles Cciv applicables.</li>
 *   <li>{@code messages} — informations contextuelles.</li>
 *   <li>{@code alertes} — points d'attention bloquants ou de vigilance.</li>
 * </ul>
 */
public record IndigniteSuccessoraleResult(
        String typeIndignite,
        String verdictIndignite,
        boolean pardonNeutralisant,
        boolean representationPossible,
        String delaiAction,
        boolean delaiForclos,
        String effetDevolution,
        String baseLegale,
        List<String> messages,
        List<String> alertes
) {}
