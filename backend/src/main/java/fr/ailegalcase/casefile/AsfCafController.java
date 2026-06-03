package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-222-01 : endpoint REST pour l'outil ASF (allocation de soutien familial,
 * art. L. 523-1 CSS — F-FA-ASF-CAF, FRANCE).
 * POST/GET /api/v1/case-files/{caseFileId}/asf-caf-analysis
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/asf-caf-analysis")
public class AsfCafController {

    private final AsfCafService service;

    public AsfCafController(AsfCafService service) {
        this.service = service;
    }

    @PostMapping
    public AsfCafResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody AsfCafRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AsfCafResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
