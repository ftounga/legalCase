package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/immigration/title-decision")
public class ImmigrationTitleDecisionController {

    private final ImmigrationTitleDecisionService decisionService;

    public ImmigrationTitleDecisionController(ImmigrationTitleDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @PostMapping
    public ImmigrationTitleDecisionResponse resolve(@PathVariable UUID caseFileId,
                                                     @RequestBody ImmigrationTitleDecisionRequest request,
                                                     @AuthenticationPrincipal OidcUser oidcUser,
                                                     Principal principal) {
        return decisionService.resolve(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ImmigrationTitleDecisionResponse get(@PathVariable UUID caseFileId,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return decisionService.get(caseFileId, oidcUser, principal);
    }
}
