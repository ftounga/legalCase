package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/avantages-conventionnels-be")
public class AvantagesConventionnelsBeController {

    private final AvantagesConventionnelsBeService service;

    public AvantagesConventionnelsBeController(AvantagesConventionnelsBeService service) {
        this.service = service;
    }

    @PostMapping
    public AvantagesConventionnelsBeResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody AvantagesConventionnelsBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AvantagesConventionnelsBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
