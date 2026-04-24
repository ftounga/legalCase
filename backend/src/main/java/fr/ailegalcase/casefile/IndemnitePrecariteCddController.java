package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/cdd-indemnite-precarite")
public class IndemnitePrecariteCddController {

    private final IndemnitePrecariteCddService service;

    public IndemnitePrecariteCddController(IndemnitePrecariteCddService service) {
        this.service = service;
    }

    @PostMapping
    public IndemnitePrecariteCddResponse calculate(@PathVariable UUID caseFileId,
                                                   @RequestBody IndemnitePrecariteCddRequest request,
                                                   @AuthenticationPrincipal OidcUser oidcUser,
                                                   Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public IndemnitePrecariteCddResponse get(@PathVariable UUID caseFileId,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
