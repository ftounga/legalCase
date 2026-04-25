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
 * SF-DT-30-01 : endpoints REST pour l'outil "Protection des représentants du personnel"
 * (FR — art. L.2411-1 et s. Code du travail).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/protection-rp-analysis")
public class ProtectionRpController {

    private final ProtectionRpService service;

    public ProtectionRpController(ProtectionRpService service) {
        this.service = service;
    }

    @PostMapping
    public ProtectionRpResponse calculate(@PathVariable UUID caseFileId,
                                          @RequestBody ProtectionRpRequest request,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ProtectionRpResponse get(@PathVariable UUID caseFileId,
                                    @AuthenticationPrincipal OidcUser oidcUser,
                                    Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
