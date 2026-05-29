package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-35 : endpoints POST/GET pour l'analyse de validité d'une mesure
 * d'assignation à résidence (art. L. 731-1 CESEDA). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/assignation-residence-analysis")
public class AssignationResidenceController {

    private final AssignationResidenceService service;

    public AssignationResidenceController(AssignationResidenceService service) {
        this.service = service;
    }

    @PostMapping
    public AssignationResidenceResponse analyze(@PathVariable UUID caseFileId,
                                                @RequestBody AssignationResidenceRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AssignationResidenceResponse get(@PathVariable UUID caseFileId,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
