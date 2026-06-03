package fr.ailegalcase.casefile;

/**
 * SF-222-03 : lien familial éligible à l'habilitation familiale (art. 494-1 Cciv).
 *
 * <p>L'habilitation familiale est ouverte aux ascendants, descendants, frères et
 * sœurs, conjoint, partenaire de PACS ou concubin du majeur à protéger. Tout
 * autre lien ({@code AUTRE}) n'est pas éligible à l'habilitation familiale et
 * doit être orienté vers une mesure judiciaire (F-FA-25).</p>
 */
public enum LienFamilialHabilitationEnum {
    ASCENDANT,
    DESCENDANT,
    FRERE_SOEUR,
    CONJOINT_PARTENAIRE,
    AUTRE
}
