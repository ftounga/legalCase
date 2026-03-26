package fr.ailegalcase.analysis;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisDiffResponse(
        VersionInfo from,
        VersionInfo to,
        SectionDiff<String> faits,
        SectionDiff<String> pointsJuridiques,
        SectionDiff<String> risques,
        SectionDiff<String> questionsOuvertes,
        SectionDiff<TimelineEntry> timeline
) {

    public record VersionInfo(UUID id, int version, String analysisType, Instant updatedAt) {}

    public record SectionDiff<T>(List<T> added, List<T> removed, List<T> unchanged) {}

    public record TimelineEntry(String date, String evenement) {}
}
