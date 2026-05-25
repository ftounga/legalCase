package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-31 : réponse de l'endpoint de vérification de conformité de la
 * procédure d'élections CSE (F-DT-65-elections-cse-conformite, FRANCE —
 * L. 2314-1 à L. 2314-37 CT ; R. 2314-1+ CT ; ordonnances Macron 22/09/2017).
 *
 * <p>Inclut un snapshot complet des inputs (pour ré-édition du formulaire
 * UI lors du GET) ET les sorties calculées (verdict 3 niveaux, score,
 * points d'irrégularité F-IA-03, délai de contestation 15 jours,
 * date limite éventuelle, alerte délai court, bases juridiques, messages).</p>
 */
public record ElectionsCseConformiteResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        Integer effectifEntreprise,
        Boolean papNegocieAvecOS,
        Boolean delaiInvitationOSRespectee,
        Boolean collegesConformes,
        Boolean resultatsContestes,
        LocalDate dateElection,
        String motifContestation,
        // --- Outputs calculés ---
        ElectionsCseConformiteCalculator.AnalyseElectionsCse analyseElectionsCse,
        int scoreConformite,
        List<ElectionsCseConformiteCalculator.PointIrregularite> pointsIrregularite,
        int delaiContestationJours,
        LocalDate dateLimitContestationSiConnue,
        boolean alerteDelaiContestation,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
