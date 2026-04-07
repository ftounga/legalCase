package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

public record ImmigrationTitleDecisionResponse(
        UUID caseFileId,
        String country,
        boolean nationaliteUe,
        String motif,
        String duree,
        String situationFamiliale,
        List<TitleRecommendation> recommendations
) {}
