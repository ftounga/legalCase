package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/immigration/work-right")
public class ImmigrationWorkRightController {

    private final ImmigrationWorkRightService workRightService;

    public ImmigrationWorkRightController(ImmigrationWorkRightService workRightService) {
        this.workRightService = workRightService;
    }

    @PostMapping
    public ImmigrationWorkRightResponse resolve(@PathVariable UUID caseFileId,
                                                 @RequestBody ImmigrationWorkRightRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return workRightService.resolve(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ImmigrationWorkRightResponse get(@PathVariable UUID caseFileId,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return workRightService.get(caseFileId, oidcUser, principal);
    }
}
