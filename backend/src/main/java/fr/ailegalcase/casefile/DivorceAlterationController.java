package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/divorce-alteration")
public class DivorceAlterationController {

    private final DivorceAlterationService service;

    public DivorceAlterationController(DivorceAlterationService service) {
        this.service = service;
    }

    @PostMapping
    public DivorceAlterationResponse calculate(@PathVariable UUID caseFileId,
                                               @RequestBody DivorceAlterationRequest request,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DivorceAlterationResponse get(@PathVariable UUID caseFileId,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
