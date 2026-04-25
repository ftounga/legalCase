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
 * SF-DT-16-01 : endpoints REST pour la détection automatique de protections + indemnité
 * plancher 6 mois (licenciement nul — art. L.1235-3-1 al. 2 — FRANCE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/licenciement-nul-detection")
public class LicenciementNulDetectionController {

    private final LicenciementNulDetectionService service;

    public LicenciementNulDetectionController(LicenciementNulDetectionService service) {
        this.service = service;
    }

    @PostMapping
    public LicenciementNulDetectionResponse calculate(@PathVariable UUID caseFileId,
                                                      @RequestBody LicenciementNulDetectionRequest request,
                                                      @AuthenticationPrincipal OidcUser oidcUser,
                                                      Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public LicenciementNulDetectionResponse get(@PathVariable UUID caseFileId,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
