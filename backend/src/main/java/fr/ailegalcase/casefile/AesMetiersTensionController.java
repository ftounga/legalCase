package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/aes-metiers-tension")
public class AesMetiersTensionController {

    private final AesMetiersTensionService service;

    public AesMetiersTensionController(AesMetiersTensionService service) {
        this.service = service;
    }

    @PostMapping
    public AesMetiersTensionResponse calculate(@PathVariable UUID caseFileId,
                                               @RequestBody AesMetiersTensionRequest request,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AesMetiersTensionResponse get(@PathVariable UUID caseFileId,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
