package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/credit-temps-be-analysis")
public class CreditTempsBeController {

    private final CreditTempsBeService service;

    public CreditTempsBeController(CreditTempsBeService service) {
        this.service = service;
    }

    @PostMapping
    public CreditTempsBeResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody CreditTempsBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CreditTempsBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
