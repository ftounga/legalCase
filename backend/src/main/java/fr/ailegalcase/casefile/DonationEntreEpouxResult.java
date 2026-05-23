package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-23 : résultat du calcul Donation entre époux FR (art. 1091-1100
 * Cciv + art. 265 al. 2 Cciv + art. 1527 al. 2 Cciv + art. 912-928 Cciv).
 *
 * <ul>
 *   <li>{@code validite} — "VALIDE" si la donation est juridiquement
 *       opposable au jour de l'analyse, "REVOQUEE" si elle est révoquée
 *       (divorce art. 265, révocation expresse/tacite art. 1096 al. 2),
 *       "A_VERIFIER" si une condition reste à constater.</li>
 *   <li>{@code revocabilite} — "REVOCABLE_AD_NUTUM" pour la donation au
 *       dernier vivant (biens futurs), "REVOCATION_DE_PLEIN_DROIT" en cas
 *       de divorce, "REVOCABILITE_LIMITEE" pour la donation de biens
 *       présents intégrée au contrat de mariage, "IRREVOCABLE" pour
 *       l'avantage matrimonial constitué au contrat de mariage.</li>
 *   <li>{@code effetDissolution} — texte décrivant l'effet de la
 *       dissolution du mariage (divorce, décès) sur la donation.</li>
 *   <li>{@code actionRetranchementPossible} — true si l'action en
 *       retranchement (art. 1527 al. 2 Cciv) est ouverte aux enfants non
 *       communs (clause attribution intégrale + enfants non communs).</li>
 *   <li>{@code impactReserve} — texte décrivant l'impact sur la réserve
 *       héréditaire (art. 912-928 Cciv) — notamment pour la donation de
 *       biens futurs.</li>
 *   <li>{@code baseLegale} — articles Cciv applicables.</li>
 *   <li>{@code messages} — informations contextuelles.</li>
 *   <li>{@code alertes} — points d'attention bloquants ou de vigilance.</li>
 * </ul>
 */
public record DonationEntreEpouxResult(
        String validite,
        String revocabilite,
        String effetDissolution,
        boolean actionRetranchementPossible,
        String impactReserve,
        String baseLegale,
        List<String> messages,
        List<String> alertes
) {}
