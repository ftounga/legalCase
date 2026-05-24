package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-13 : réponse de l'endpoint d'analyse de la validité d'une clause de
 * mobilité et des conséquences d'un refus de mutation
 * (F-DT-71-mutation-clause-mobilite, FRANCE — Cass. soc. constante).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI lors du
 * GET) ET les sorties calculées (verdict, score, points d'analyse,
 * conséquences du refus, bases juridiques).</p>
 */
public record MutationClauseMobiliteResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        boolean clauseMobilitePresente,
        boolean zoneGeographiquePrecise,
        boolean interetLegitimeEmployeur,
        Integer delaiPrevenanceSemaines,
        boolean situationFamilialeSalarieContraignante,
        boolean motifMutationProfessionnel,
        MutationClauseMobiliteInput.ReponseSalarie reponseSalarie,
        // --- Outputs calculés ---
        MutationClauseMobiliteCalculator.AnalyseMutation analyseMutation,
        int scoreMutation,
        List<MutationClauseMobiliteCalculator.PointAnalyse> pointsAnalyse,
        List<MutationClauseMobiliteCalculator.Consequence> consequences,
        String consequencesRefus,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
