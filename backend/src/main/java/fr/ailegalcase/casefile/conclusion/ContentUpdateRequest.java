package fr.ailegalcase.casefile.conclusion;

/**
 * F-98 / SF-98-49 — corps du {@code PATCH .../conclusions/versions/{versionId}/content}.
 *
 * <p>Permet à l'avocat de modifier le texte ({@code content}) d'une version de
 * conclusions en brouillon avant de la valider. Le champ est typé {@code String}
 * pour que la garde « content vide/absent » soit faite dans le service et renvoie
 * un {@code 400} au message explicite.</p>
 *
 * @param content le nouveau texte des conclusions — non vide
 */
public record ContentUpdateRequest(String content) {
}
