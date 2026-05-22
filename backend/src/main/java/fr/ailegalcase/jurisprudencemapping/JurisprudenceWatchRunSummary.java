package fr.ailegalcase.jurisprudencemapping;

import java.time.LocalDate;

/**
 * F-JU-01 / SF-JU-01-02 — compteurs d'un run mensuel pour email récap.
 */
public record JurisprudenceWatchRunSummary(
        LocalDate periodStartInclusive,
        LocalDate periodEndExclusive,
        int arretsRecuperes,
        int mappingsEvalues,
        int autoConfirm,
        int autoAdd,
        int autoReplace,
        int autoArchive,
        int flagsPending,
        int skipped,
        boolean aborted,
        String abortReason) {
}
