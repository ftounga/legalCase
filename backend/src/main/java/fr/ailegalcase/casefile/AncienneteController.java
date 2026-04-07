package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/anciennete")
public class AncienneteController {

    private final AncienneteService ancienneteService;

    public AncienneteController(AncienneteService ancienneteService) {
        this.ancienneteService = ancienneteService;
    }

    @PostMapping
    public AncienneteResponse calculate(@PathVariable UUID caseFileId,
                                         @RequestBody AncienneteRequest request,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return ancienneteService.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AncienneteResponse get(@PathVariable UUID caseFileId,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return ancienneteService.get(caseFileId, oidcUser, principal);
    }
}
