package fr.ailegalcase.casefile;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-238-03 : endpoints d'activation manuelle d'outils décisionnels par
 * l'avocat.
 *
 * <p>POST → active un outil (200 Created body avec id/toolId/activatedAt).</p>
 * <p>DELETE → désactive un outil (204, idempotent).</p>
 *
 * <p>L'activation manuelle est consommée par {@link DecisionToolVisibilityService}
 * qui injecte les tool_id activés dans la liste {@code contextual} (et les
 * retire de {@code catalog}) au prochain GET de la visibilité.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools-visibility/manual-activations")
public class ManualToolActivationController {

    private final ManualToolActivationService service;

    public ManualToolActivationController(ManualToolActivationService service) {
        this.service = service;
    }

    @PostMapping
    public ManualToolActivationResponse activate(@PathVariable UUID caseFileId,
                                                 @RequestBody ManualToolActivationRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.activate(caseFileId, request, oidcUser, principal);
    }

    @DeleteMapping("/{toolId}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID caseFileId,
                                           @PathVariable String toolId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        service.deactivate(caseFileId, toolId, oidcUser, principal);
        return ResponseEntity.noContent().build();
    }
}
