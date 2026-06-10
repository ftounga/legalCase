package fr.ailegalcase.casefile.conclusion;

/**
 * F-265 / SF-265-01 — réponse {@code 200} du
 * {@code POST .../conclusions/versions/{versionId}/sections/regenerate}.
 *
 * <p>Contient uniquement le markdown régénéré de la section. <strong>Aucune
 * persistance</strong> : le frontend insère ce markdown dans l'éditeur
 * (round-trip markdown F-264) ; l'avocat relit puis enregistre via l'endpoint
 * {@code PATCH .../content} existant.</p>
 *
 * @param regeneratedMarkdown le markdown régénéré de la section
 */
public record SectionRegenerationResponse(String regeneratedMarkdown) {
}
