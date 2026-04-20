package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/rupture-conv-indemnite")
public class RuptureConvIndemniteController {

    private final RuptureConvIndemniteService service;

    public RuptureConvIndemniteController(RuptureConvIndemniteService service) {
        this.service = service;
    }

    @PostMapping
    public RuptureConvIndemniteResponse calculate(@PathVariable UUID caseFileId,
                                                  @RequestBody RuptureConvIndemniteRequest request,
                                                  @AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RuptureConvIndemniteResponse get(@PathVariable UUID caseFileId,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
