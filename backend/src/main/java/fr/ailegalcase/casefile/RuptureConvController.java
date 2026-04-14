package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/rupture-conv")
public class RuptureConvController {

    private final RuptureConvService service;

    public RuptureConvController(RuptureConvService service) {
        this.service = service;
    }

    @PostMapping
    public RuptureConvResponse analyze(@PathVariable UUID caseFileId,
                                        @RequestBody RuptureConvRequest request,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RuptureConvResponse get(@PathVariable UUID caseFileId,
                                    @AuthenticationPrincipal OidcUser oidcUser,
                                    Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
