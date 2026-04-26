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
 * SF-FA-18-03 : endpoints REST pour l'outil "Contestation de paternité"
 * (FR — DROIT_FAMILLE — art. 332-335 + 311-1 + 321 + 372 Cciv).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/contestation-paternite-analysis")
public class ContestationPaterniteController {

    private final ContestationPaterniteService service;

    public ContestationPaterniteController(ContestationPaterniteService service) {
        this.service = service;
    }

    @PostMapping
    public ContestationPaterniteResponse calculate(@PathVariable UUID caseFileId,
                                                   @RequestBody ContestationPaterniteRequest request,
                                                   @AuthenticationPrincipal OidcUser oidcUser,
                                                   Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ContestationPaterniteResponse get(@PathVariable UUID caseFileId,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
