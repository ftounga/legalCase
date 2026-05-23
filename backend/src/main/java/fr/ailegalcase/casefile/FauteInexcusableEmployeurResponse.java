package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-09 : réponse de l'endpoint d'évaluation de la faute inexcusable
 * de l'employeur (F-DT-91, FRANCE — L. 452-1 à L. 452-5 CSS ; Cass. ass.
 * plén. 24/06/2005 ; L. 4121-1 CT).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI lors
 * du GET) ET les sorties calculées (verdict, score, facteurs, majoration
 * rente, alerte procédure pôle social, bases juridiques).</p>
 *
 * <p>L'<b>alerte procédure pôle social</b>
 * ({@code alerteProcedurePolesSocial}) est <b>toujours renseignée</b>
 * — invariant de la mini-spec : la distinction procédurale TJ ≠ CPH ne
 * doit jamais être oubliée.</p>
 */
public record FauteInexcusableEmployeurResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        Boolean conscienceDangerEmployeurEtablie,
        Boolean signalementDangerPrior,
        Boolean mesuresPreventionPrises,
        Boolean documentUniqueEvalue,
        Boolean formationSecuriteProdiguee,
        double tauxIpp,
        Double renteMensuelleEuros,
        double salaireMensuelBrutEuros,
        // --- Outputs calculés ---
        FauteInexcusableEmployeurCalculator.EvaluationFauteInexcusable evaluationFauteInexcusable,
        int scoreFauteInexcusable,
        List<FauteInexcusableEmployeurCalculator.FacteurFauteInexcusable> facteursFauteInexcusable,
        Double majorationRenteEstimeeEuros,
        String alerteProcedurePolesSocial,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
