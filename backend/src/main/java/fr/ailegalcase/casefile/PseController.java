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
 * SF-DT-14-01 : endpoints REST pour l'outil "PSE — critères de validité"
 * (FR — art. L.1233-24-1 et s.).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/pse-analysis")
public class PseController {

    private final PseService service;

    public PseController(PseService service) {
        this.service = service;
    }

    @PostMapping
    public PseResponse calculate(@PathVariable UUID caseFileId,
                                 @RequestBody PseRequest request,
                                 @AuthenticationPrincipal OidcUser oidcUser,
                                 Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PseResponse get(@PathVariable UUID caseFileId,
                           @AuthenticationPrincipal OidcUser oidcUser,
                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
