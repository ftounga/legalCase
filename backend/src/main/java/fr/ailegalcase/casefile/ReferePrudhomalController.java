package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/refere-prudhomal")
public class ReferePrudhomalController {

    private final ReferePrudhomalService service;

    public ReferePrudhomalController(ReferePrudhomalService service) {
        this.service = service;
    }

    @PostMapping
    public ReferePrudhomalResponse calculate(@PathVariable UUID caseFileId,
                                             @RequestBody ReferePrudhomalRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ReferePrudhomalResponse get(@PathVariable UUID caseFileId,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
