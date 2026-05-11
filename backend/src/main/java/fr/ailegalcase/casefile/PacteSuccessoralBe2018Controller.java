package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/pacte-successoral-be-2018-analysis")
public class PacteSuccessoralBe2018Controller {

    private final PacteSuccessoralBe2018Service service;

    public PacteSuccessoralBe2018Controller(PacteSuccessoralBe2018Service service) {
        this.service = service;
    }

    @PostMapping
    public PacteSuccessoralBe2018Response calculate(@PathVariable UUID caseFileId,
                                                    @RequestBody PacteSuccessoralBe2018Request request,
                                                    @AuthenticationPrincipal OidcUser oidcUser,
                                                    Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PacteSuccessoralBe2018Response get(@PathVariable UUID caseFileId,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
