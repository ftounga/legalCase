package fr.ailegalcase.search;

import java.util.List;

public record SynthesisSearchResponse(
        String query,
        int totalResults,
        List<SynthesisSearchResult> results
) {}
