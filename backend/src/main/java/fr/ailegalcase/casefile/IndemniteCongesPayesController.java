package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-DT-26-01 : controller exposant l'outil Indemnite compensatrice conges payes
 * (art. L.3141-26 et L.3141-28 Code du travail).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/conges-payes-indemnite")
public class IndemniteCongesPayesController {

    private final IndemniteCongesPayesService service;

    public IndemniteCongesPayesController(IndemniteCongesPayesService service) {
        this.service = service;
    }

    @PostMapping
    public IndemniteCongesPayesResponse calculate(@PathVariable UUID caseFileId,
                                                  @RequestBody IndemniteCongesPayesRequest request,
                                                  @AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public IndemniteCongesPayesResponse get(@PathVariable UUID caseFileId,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
