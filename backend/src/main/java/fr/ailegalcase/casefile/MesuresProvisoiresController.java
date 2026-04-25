package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-12-01 : endpoint REST des mesures provisoires (AOMP, art. 254 Cciv).
 * Single-country FR DROIT_FAMILLE.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/mesures-provisoires")
public class MesuresProvisoiresController {

    private final MesuresProvisoiresService service;

    public MesuresProvisoiresController(MesuresProvisoiresService service) {
        this.service = service;
    }

    @PostMapping
    public MesuresProvisoiresResponse calculate(@PathVariable UUID caseFileId,
                                                @RequestBody MesuresProvisoiresRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public MesuresProvisoiresResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
