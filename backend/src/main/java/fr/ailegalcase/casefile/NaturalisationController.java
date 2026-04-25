package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-IM-13-01 : controller HTTP pour l'analyse de naturalisation française (Code civil 21+).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/naturalisation-analysis")
public class NaturalisationController {

    private final NaturalisationService service;

    public NaturalisationController(NaturalisationService service) {
        this.service = service;
    }

    @PostMapping
    public NaturalisationResponse calculate(@PathVariable UUID caseFileId,
                                            @RequestBody NaturalisationRequest request,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public NaturalisationResponse get(@PathVariable UUID caseFileId,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
