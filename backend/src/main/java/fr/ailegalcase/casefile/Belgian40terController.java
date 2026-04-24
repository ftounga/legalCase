package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/belgian-40ter")
public class Belgian40terController {

    private final Belgian40terService service;

    public Belgian40terController(Belgian40terService service) {
        this.service = service;
    }

    @PostMapping
    public Belgian40terResponse calculate(@PathVariable UUID caseFileId,
                                          @RequestBody Belgian40terRequest request,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public Belgian40terResponse get(@PathVariable UUID caseFileId,
                                    @AuthenticationPrincipal OidcUser oidcUser,
                                    Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
