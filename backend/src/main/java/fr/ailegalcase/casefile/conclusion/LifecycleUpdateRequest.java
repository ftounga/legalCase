package fr.ailegalcase.casefile.conclusion;

/**
 * F-98 / SF-98-52 — corps du {@code PATCH .../conclusions/versions/{versionId}/lifecycle}.
 *
 * <p>Le champ est typé {@code String} (et non {@link ConclusionLifecycleStatus}) afin
 * que la validation d'une valeur inconnue soit faite dans le service et renvoie un
 * {@code 400} au message explicite plutôt qu'une erreur de désérialisation Jackson.</p>
 *
 * @param lifecycleStatus une valeur de {@link ConclusionLifecycleStatus} ({@code DRAFT},
 *                        {@code VALIDATED}, {@code DEPOSITED})
 */
public record LifecycleUpdateRequest(String lifecycleStatus) {
}
