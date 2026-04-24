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
 * SF-DT-15-01 : endpoints REST pour l'outil "Licenciement pour inaptitude" (FR + BE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/inaptitude")
public class InaptitudeController {

    private final InaptitudeService service;

    public InaptitudeController(InaptitudeService service) {
        this.service = service;
    }

    @PostMapping
    public InaptitudeResponse calculate(@PathVariable UUID caseFileId,
                                        @RequestBody InaptitudeRequest request,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public InaptitudeResponse get(@PathVariable UUID caseFileId,
                                  @AuthenticationPrincipal OidcUser oidcUser,
                                  Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
