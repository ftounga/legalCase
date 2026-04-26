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
 * SF-FA-24-01 : endpoints REST pour l'outil "Dévolution légale successorale"
 * (FR — DROIT_FAMILLE — art. 731 et s. Cciv).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/devolution-legale-analysis")
public class DevolutionLegaleController {

    private final DevolutionLegaleService service;

    public DevolutionLegaleController(DevolutionLegaleService service) {
        this.service = service;
    }

    @PostMapping
    public DevolutionLegaleResponse calculate(@PathVariable UUID caseFileId,
                                              @RequestBody DevolutionLegaleRequest request,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DevolutionLegaleResponse get(@PathVariable UUID caseFileId,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
