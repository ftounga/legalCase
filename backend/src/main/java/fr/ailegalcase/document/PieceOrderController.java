package fr.ailegalcase.document;

import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * F-260 / SF-260-01 : réordonnancement explicite des pièces d'un dossier.
 * Endpoint au niveau dossier (et non document) car l'ordre porte sur l'ensemble
 * des pièces tous documents confondus.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/pieces")
public class PieceOrderController {

    private final PieceOrderService pieceOrderService;

    public PieceOrderController(PieceOrderService pieceOrderService) {
        this.pieceOrderService = pieceOrderService;
    }

    /**
     * Réassigne le {@code piece_number} 1..N dans l'ordre fourni.
     * 200 + liste à jour ; 400 si la liste est invalide ; 404 cross-workspace.
     */
    @PutMapping("/order")
    public List<DocumentPieceSummary> reorder(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody ReorderPiecesRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return pieceOrderService.reorder(caseFileId, request.orderedPieceIds(),
                oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }
}
