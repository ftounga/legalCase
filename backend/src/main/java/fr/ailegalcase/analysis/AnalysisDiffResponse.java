package fr.ailegalcase.analysis;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisDiffResponse(
        VersionInfo from,
        VersionInfo to,
        SectionDiff faits,
        SectionDiff pointsJuridiques,
        SectionDiff risques,
        SectionDiff questionsOuvertes,
        TimelineSectionDiff timeline
) {

    public record VersionInfo(UUID id, int version, String analysisType, Instant updatedAt) {}

    public record DiffItem(String text, String reason) {}

    public record SectionDiff(
            List<DiffItem> added,
            List<DiffItem> removed,
            List<DiffItem> unchanged,
            List<DiffItem> enriched) {}

    public record TimelineDiffItem(String date, String evenement, String reason) {}

    public record TimelineSectionDiff(
            List<TimelineDiffItem> added,
            List<TimelineDiffItem> removed,
            List<TimelineDiffItem> unchanged,
            List<TimelineDiffItem> enriched) {}
}
