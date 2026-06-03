package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-222-04 : endpoint REST pour l'outil assistance éducative — mineur en danger
 * (art. 375 et s. Cciv — F-FA-ASSISTANCE-EDUCATIVE, FRANCE).
 * POST/GET /api/v1/case-files/{caseFileId}/assistance-educative-analysis
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/assistance-educative-analysis")
public class AssistanceEducativeController {

    private final AssistanceEducativeService service;

    public AssistanceEducativeController(AssistanceEducativeService service) {
        this.service = service;
    }

    @PostMapping
    public AssistanceEducativeResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody AssistanceEducativeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AssistanceEducativeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
