package fr.ailegalcase.jurisprudencemapping;

/**
 * F-JU-01 / SF-JU-01-03 — compteurs d'un run du cron dérive quotidienne.
 */
public record JurisprudenceDriftRunSummary(
        int mappingsActifsTotal,
        int orphelinsArchives,
        boolean aborted,
        String abortReason) {

    public static JurisprudenceDriftRunSummary aborted(int mappingsActifs, String reason) {
        return new JurisprudenceDriftRunSummary(mappingsActifs, 0, true, reason);
    }
}
