package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-DT-25-01 : endpoints REST pour l'outil décisionnel "Indemnité compensatrice de préavis FR".
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/indemnite-preavis")
public class IndemnitePreavisController {

    private final IndemnitePreavisService service;

    public IndemnitePreavisController(IndemnitePreavisService service) {
        this.service = service;
    }

    @PostMapping
    public IndemnitePreavisResponse calculate(@PathVariable UUID caseFileId,
                                              @RequestBody IndemnitePreavisRequest request,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public IndemnitePreavisResponse get(@PathVariable UUID caseFileId,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
