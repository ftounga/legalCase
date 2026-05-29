package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-07 : endpoints POST/GET pour l'analyse du délai de validation du
 * VLS-TS auprès de l'OFII (art. R. 311-3 CESEDA). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/vls-ts-validation-analysis")
public class VlsTsValidationController {

    private final VlsTsValidationService service;

    public VlsTsValidationController(VlsTsValidationService service) {
        this.service = service;
    }

    @PostMapping
    public VlsTsValidationResponse analyze(@PathVariable UUID caseFileId,
                                           @RequestBody VlsTsValidationRequest request,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public VlsTsValidationResponse get(@PathVariable UUID caseFileId,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
