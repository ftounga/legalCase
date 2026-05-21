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
        String country,
        // SF-252b-01 — Audit F-DT-38 2026-05-20
        /** Durée légale max en jours (barèmes exacts CDD L.1242-10 / INTERIM L.1251-14). */
        int dureeLegaleMaximaleJours,
        /** Indemnité compensatrice de préavis non exécuté L.1221-25 (Cass. soc. 23/01/2013). */
        Double indemnitePrevenanceEuros
) {}
