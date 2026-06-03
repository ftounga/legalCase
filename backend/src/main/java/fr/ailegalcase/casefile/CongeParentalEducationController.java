package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-45 : endpoints POST/GET pour l'analyse du congé parental d'éducation
 * (art. L.1225-47 à L.1225-60 CT, F-DT-78). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/conge-parental-education-analysis")
public class CongeParentalEducationController {

    private final CongeParentalEducationService service;

    public CongeParentalEducationController(CongeParentalEducationService service) {
        this.service = service;
    }

    @PostMapping
    public CongeParentalEducationResponse analyze(@PathVariable UUID caseFileId,
                                                  @RequestBody CongeParentalEducationRequest request,
                                                  @AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CongeParentalEducationResponse get(@PathVariable UUID caseFileId,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
