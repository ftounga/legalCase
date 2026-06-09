package fr.ailegalcase.document;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * F-260 / SF-260-01 : payload de réordonnancement explicite des pièces d'un dossier.
 * La liste doit contenir <b>exactement</b> l'ensemble des pièces du dossier, dans
 * l'ordre cible ; {@code piece_number} est réassigné 1..N selon cet ordre.
 */
public record ReorderPiecesRequest(
        @NotEmpty(message = "orderedPieceIds is required")
        List<UUID> orderedPieceIds
) {}
