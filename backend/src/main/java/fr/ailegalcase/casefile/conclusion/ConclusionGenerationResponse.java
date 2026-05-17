package fr.ailegalcase.casefile.conclusion;

/**
 * F-98 / SF-98-01 + SF-98-52 — corps de la réponse {@code 202} du déclenchement
 * {@code POST .../conclusions/generate}. Contrat figé :
 * {@code {"status":"PENDING","versionNumber":N}} où {@code N} est le numéro de la
 * version nouvellement créée.
 */
public record ConclusionGenerationResponse(String status, int versionNumber) {

    /** Réponse pour une nouvelle version au numéro {@code versionNumber}. */
    public static ConclusionGenerationResponse pending(int versionNumber) {
        return new ConclusionGenerationResponse(CaseConclusionStatus.PENDING.name(), versionNumber);
    }
}
