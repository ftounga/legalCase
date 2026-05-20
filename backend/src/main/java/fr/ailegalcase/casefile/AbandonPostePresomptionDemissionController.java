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
 * SF-206-01 : endpoints REST pour l'analyse de contestation d'une présomption
 * de démission par abandon de poste (FR — loi 21/12/2022).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/abandon-poste-presomption-demission")
public class AbandonPostePresomptionDemissionController {

    private final AbandonPostePresomptionDemissionService service;

    public AbandonPostePresomptionDemissionController(AbandonPostePresomptionDemissionService service) {
        this.service = service;
    }

    @PostMapping
    public AbandonPostePresomptionDemissionResponse calculate(@PathVariable UUID caseFileId,
                                                              @RequestBody AbandonPostePresomptionDemissionRequest request,
                                                              @AuthenticationPrincipal OidcUser oidcUser,
                                                              Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AbandonPostePresomptionDemissionResponse get(@PathVariable UUID caseFileId,
                                                        @AuthenticationPrincipal OidcUser oidcUser,
                                                        Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
