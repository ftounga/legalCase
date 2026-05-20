package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-DT-38-01 : résultat structuré de la qualification d'une rupture pendant
 * la période d'essai (FR).
 */
public record RupturePeriodeEssaiResult(
        List<RupturePeriodeEssaiCalculator.Anomalie> anomaliesDetectees,
        int scoreIrregularite,
        RupturePeriodeEssaiCalculator.Verdict verdict,
        int ancienneteJoursAuMomentRupture,
        int dureeLegaleMaximaleMois,
        int delaiPrevenanceLegalJours,
        boolean delaiPrevenanceRespecte,
        RupturePeriodeEssaiCalculator.IndemniteEstimee indemniteEstimee,
        boolean remedeReintegration,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
