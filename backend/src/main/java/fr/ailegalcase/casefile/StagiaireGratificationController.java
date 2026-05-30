package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-21 : endpoints POST/GET pour l'analyse du régime du stagiaire
 * (gratification minimale + requalification en CDI, art. L.124-1 et s. du code
 * de l'éducation, F-DT-109). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/stagiaire-gratification-analysis")
public class StagiaireGratificationController {

    private final StagiaireGratificationService service;

    public StagiaireGratificationController(StagiaireGratificationService service) {
        this.service = service;
    }

    @PostMapping
    public StagiaireGratificationResponse analyze(@PathVariable UUID caseFileId,
                                                  @RequestBody StagiaireGratificationRequest request,
                                                  @AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public StagiaireGratificationResponse get(@PathVariable UUID caseFileId,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
