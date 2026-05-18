package fr.ailegalcase.casefile.jurisprudence;

import java.util.List;

/**
 * F-242 / SF-242-01 — enveloppe du {@code GET .../jurisprudence-citations}.
 *
 * @param citations citations du dossier, regroupées par point juridique (ordre stable)
 */
public record JurisprudenceCitationListResponse(List<JurisprudenceCitationResponse> citations) {
}
