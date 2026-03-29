package fr.ailegalcase.search;

import java.util.List;
import java.util.UUID;

public record SynthesisSearchResult(
        UUID caseFileId,
        String caseFileTitle,
        String legalDomain,
        String analysisType,
        int matchCount,
        List<String> excerpts
) {}
