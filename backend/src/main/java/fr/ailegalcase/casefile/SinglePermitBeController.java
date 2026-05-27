package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/single-permit-be-analysis")
public class SinglePermitBeController {

    private final SinglePermitBeService service;

    public SinglePermitBeController(SinglePermitBeService service) {
        this.service = service;
    }

    @PostMapping
    public SinglePermitBeResponse calculate(@PathVariable UUID caseFileId,
                                            @RequestBody SinglePermitBeRequest request,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public SinglePermitBeResponse get(@PathVariable UUID caseFileId,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
