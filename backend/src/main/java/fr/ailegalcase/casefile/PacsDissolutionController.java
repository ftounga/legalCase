package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/pacs-dissolution")
public class PacsDissolutionController {

    private final PacsDissolutionService service;

    public PacsDissolutionController(PacsDissolutionService service) {
        this.service = service;
    }

    @PostMapping
    public PacsDissolutionResponse calculate(@PathVariable UUID caseFileId,
                                             @RequestBody PacsDissolutionRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PacsDissolutionResponse get(@PathVariable UUID caseFileId,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
