package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-13 : résultat du calcul Audition du mineur par le JAF (art. 388-1
 * Cciv + art. 1074-1 à 1074-3 CPC).
 *
 * <ul>
 *   <li>{@code conditionsRemplies} — true si les conditions de l'audition
 *       (discernement + demande, ou demande de l'enfant lui-même) sont
 *       réunies au sens de l'art. 388-1 Cciv.</li>
 *   <li>{@code droitAuditionReconnu} — true si l'enfant a un droit propre
 *       à être entendu (notamment art. 388-1 al. 1 — l'enfant peut
 *       demander lui-même son audition, qui est alors de droit).</li>
 *   <li>{@code modaliteRecommandee} — modalité d'audition recommandée
 *       compte tenu du contexte (SEUL / AVEC_AVOCAT / AVEC_TIERS).</li>
 *   <li>{@code refusContestable} — true si un refus du juge a été notifié
 *       sans motivation suffisante (voie de recours possible — Cass. 1ère
 *       civ., 18/3/2015).</li>
 *   <li>{@code verdict} — code du verdict ("AUDITION_DE_DROIT",
 *       "AUDITION_RECOMMANDEE", "DISCERNEMENT_DOUTEUX", "REFUS_CONTESTABLE",
 *       "AUDITION_REFUSEE_VALABLEMENT", "OK").</li>
 *   <li>{@code baseLegale} — articles Cciv et CPC applicables.</li>
 *   <li>{@code messages} — informations contextuelles.</li>
 *   <li>{@code alertes} — points d'attention bloquants ou de vigilance.</li>
 * </ul>
 */
public record AuditionMineurResult(
        boolean conditionsRemplies,
        boolean droitAuditionReconnu,
        ModaliteAuditionEnum modaliteRecommandee,
        boolean refusContestable,
        String verdict,
        String baseLegale,
        List<String> messages,
        List<String> alertes
) {}
