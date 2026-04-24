package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/aes-famille")
public class AesFamilleController {

    private final AesFamilleService service;

    public AesFamilleController(AesFamilleService service) {
        this.service = service;
    }

    @PostMapping
    public AesFamilleResponse calculate(@PathVariable UUID caseFileId,
                                        @RequestBody AesFamilleRequest request,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AesFamilleResponse get(@PathVariable UUID caseFileId,
                                  @AuthenticationPrincipal OidcUser oidcUser,
                                  Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
