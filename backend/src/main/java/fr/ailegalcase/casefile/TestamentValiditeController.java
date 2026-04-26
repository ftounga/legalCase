package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-24-03 : endpoints REST pour l'outil "Validité du testament"
 * (FR — DROIT_FAMILLE — art. 967-1035 + 901-911 Cciv).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/testament-validite-analysis")
public class TestamentValiditeController {

    private final TestamentValiditeService service;

    public TestamentValiditeController(TestamentValiditeService service) {
        this.service = service;
    }

    @PostMapping
    public TestamentValiditeResponse calculate(@PathVariable UUID caseFileId,
                                               @RequestBody TestamentValiditeRequest request,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public TestamentValiditeResponse get(@PathVariable UUID caseFileId,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
