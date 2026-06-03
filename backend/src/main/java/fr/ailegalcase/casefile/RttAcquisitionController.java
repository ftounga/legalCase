package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-49 : endpoints POST/GET pour l'outil "RTT — acquisition selon accord
 * d'aménagement" (art. L.3121-41 à L.3121-44 CT, F-DT-80). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/rtt-acquisition-analysis")
public class RttAcquisitionController {

    private final RttAcquisitionService service;

    public RttAcquisitionController(RttAcquisitionService service) {
        this.service = service;
    }

    @PostMapping
    public RttAcquisitionResponse analyze(@PathVariable UUID caseFileId,
                                          @RequestBody RttAcquisitionRequest request,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RttAcquisitionResponse get(@PathVariable UUID caseFileId,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
