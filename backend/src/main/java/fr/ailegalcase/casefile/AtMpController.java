package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-DT-33-01 : controller HTTP pour l'analyse de recevabilité AT/MP française
 * (CSS L.411-1 / L.461-1 / L.434-2).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/at-mp-analysis")
public class AtMpController {

    private final AtMpService service;

    public AtMpController(AtMpService service) {
        this.service = service;
    }

    @PostMapping
    public AtMpResponse calculate(@PathVariable UUID caseFileId,
                                  @RequestBody AtMpRequest request,
                                  @AuthenticationPrincipal OidcUser oidcUser,
                                  Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AtMpResponse get(@PathVariable UUID caseFileId,
                            @AuthenticationPrincipal OidcUser oidcUser,
                            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
