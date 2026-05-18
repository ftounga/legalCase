package fr.ailegalcase.casefile.jurisprudence;

/**
 * F-242 / SF-242-01 — corps du {@code PUT .../jurisprudence-citations/{citationId}}.
 *
 * <p>Seuls {@code reference} et {@code portee} sont éditables ; le rattachement au
 * point juridique ({@code pointJuridiqueIndex} / {@code pointJuridiqueTexte}) est figé
 * à la création. La validation est faite dans {@link JurisprudenceCitationService}.</p>
 *
 * @param reference libellé de la référence, requis, ≤ 255
 * @param portee    ligne de portée, optionnelle, ≤ 500
 */
public record UpdateJurisprudenceCitationRequest(
        String reference,
        String portee) {
}
