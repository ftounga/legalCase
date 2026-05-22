package fr.ailegalcase.jurisprudencemapping;

import java.time.LocalDate;

/**
 * F-JU-01 / SF-JU-01-02 — arrêt JUDILIBRE récupéré par {@link JudilibreApiClient}.
 *
 * <p>Champs minimum nécessaires à l'évaluation Claude et à l'écriture éventuelle
 * dans {@link ToolJurisprudenceMapping}.</p>
 *
 * @param judilibreId      identifiant interne JUDILIBRE (clé d'unicité côté API)
 * @param ref              référence formatée ex. {@code "Cass. soc. 8 janv. 2025, n° 23-12.345"}
 * @param juridiction      juridiction émettrice
 * @param dateArret        date de la décision
 * @param numeroPourvoi    numéro de pourvoi / décision
 * @param chapeauOfficiel  chapeau officiel cité textuellement (pas une reformulation)
 * @param lienLegifrance   URL canonique
 */
public record JudilibreArret(
        String judilibreId,
        String ref,
        String juridiction,
        LocalDate dateArret,
        String numeroPourvoi,
        String chapeauOfficiel,
        String lienLegifrance) {
}
