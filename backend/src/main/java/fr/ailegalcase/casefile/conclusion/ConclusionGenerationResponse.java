package fr.ailegalcase.casefile.conclusion;

/**
 * F-98 / SF-98-01 — corps de la réponse {@code 202} du déclenchement
 * {@code POST .../conclusions/generate}. Contrat figé : {@code {"status":"PENDING"}}.
 */
public record ConclusionGenerationResponse(String status) {

    public static ConclusionGenerationResponse pending() {
        return new ConclusionGenerationResponse(CaseConclusionStatus.PENDING.name());
    }
}
