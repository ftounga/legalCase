package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

public record RuptureConvResponse(
        UUID caseFileId,
        String country,
        int scoreRisque,
        String verdict,
        List<CritereData> criteres
) {
    public record CritereData(
            String code,
            String label,
            String reponse,
            int pointsRisque,
            boolean bloquant,
            String commentaire
    ) {}
}
