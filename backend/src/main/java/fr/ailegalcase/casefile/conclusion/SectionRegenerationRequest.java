package fr.ailegalcase.casefile.conclusion;

/**
 * F-265 / SF-265-01 — corps du
 * {@code POST .../conclusions/versions/{versionId}/sections/regenerate}.
 *
 * <p>Co-rédaction au paragraphe : l'avocat sélectionne une section de l'acte
 * ({@code sectionMarkdown}) et demande sa régénération selon une
 * {@code instruction} libre (« renforce la prescription », « durcis sur le
 * barème »). Les champs sont typés {@code String} pour que les gardes
 * « vide/absent » et « instruction trop longue » soient faites dans le service
 * et renvoient un {@code 400} au message explicite.</p>
 *
 * @param sectionMarkdown le markdown de la section à régénérer — non vide
 * @param instruction     la consigne de l'avocat — non vide, bornée
 */
public record SectionRegenerationRequest(String sectionMarkdown, String instruction) {
}
