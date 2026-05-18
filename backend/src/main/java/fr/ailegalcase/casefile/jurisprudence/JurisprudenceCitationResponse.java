package fr.ailegalcase.casefile.jurisprudence;

import java.time.Instant;
import java.util.UUID;

/**
 * F-242 / SF-242-01 — représentation API d'une {@link JurisprudenceCitation}.
 *
 * @param id                  identifiant de la citation
 * @param pointJuridiqueIndex index du point juridique dans la synthèse
 * @param pointJuridiqueTexte snapshot du texte du point juridique
 * @param reference           libellé de la référence
 * @param portee              ligne de portée ({@code null} si non renseignée)
 * @param createdAt           instant de création
 * @param updatedAt           instant de dernière modification
 */
public record JurisprudenceCitationResponse(
        UUID id,
        int pointJuridiqueIndex,
        String pointJuridiqueTexte,
        String reference,
        String portee,
        Instant createdAt,
        Instant updatedAt) {

    /** Mappe une entité vers sa représentation API. */
    public static JurisprudenceCitationResponse from(JurisprudenceCitation citation) {
        return new JurisprudenceCitationResponse(
                citation.getId(),
                citation.getPointJuridiqueIndex(),
                citation.getPointJuridiqueTexte(),
                citation.getReference(),
                citation.getPortee(),
                citation.getCreatedAt(),
                citation.getUpdatedAt());
    }
}
