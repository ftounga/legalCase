package fr.ailegalcase.casefile.jurisprudence;

/**
 * F-242 / SF-242-01 — corps du {@code POST .../jurisprudence-citations}.
 *
 * <p>Les champs sont volontairement non contraints par bean validation : la
 * validation est faite dans {@link JurisprudenceCitationService} pour produire un
 * {@code 400} au message explicite (contrat API figé de la mini-spec).</p>
 *
 * @param pointJuridiqueIndex index du point juridique dans la synthèse, ≥ 0, requis
 * @param pointJuridiqueTexte snapshot du texte du point juridique, requis, ≤ 2000
 * @param reference           libellé de la référence, requis, ≤ 255
 * @param portee              ligne de portée, optionnelle, ≤ 500
 */
public record CreateJurisprudenceCitationRequest(
        Integer pointJuridiqueIndex,
        String pointJuridiqueTexte,
        String reference,
        String portee) {
}
