package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/belgian-40bis")
public class Belgian40bisController {

    private final Belgian40bisService service;

    public Belgian40bisController(Belgian40bisService service) {
        this.service = service;
    }

    @PostMapping
    public Belgian40bisResponse calculate(@PathVariable UUID caseFileId,
                                          @RequestBody Belgian40bisRequest request,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public Belgian40bisResponse get(@PathVariable UUID caseFileId,
                                    @AuthenticationPrincipal OidcUser oidcUser,
                                    Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
