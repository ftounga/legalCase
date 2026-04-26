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
 * SF-FA-18-01 : endpoints REST pour l'outil "Reconnaissance paternelle"
 * (FR — DROIT_FAMILLE — art. 316 + 332-335 + 372 Cciv).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/reconnaissance-paternelle-analysis")
public class ReconnaissancePaterneleController {

    private final ReconnaissancePaterneleService service;

    public ReconnaissancePaterneleController(ReconnaissancePaterneleService service) {
        this.service = service;
    }

    @PostMapping
    public ReconnaissancePaterneleResponse calculate(@PathVariable UUID caseFileId,
                                                     @RequestBody ReconnaissancePaterneleRequest request,
                                                     @AuthenticationPrincipal OidcUser oidcUser,
                                                     Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ReconnaissancePaterneleResponse get(@PathVariable UUID caseFileId,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
