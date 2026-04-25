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
 * SF-DT-13-01 : endpoints REST pour l'outil "Licenciement économique" (FR — L.1233-x).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/licenciement-economique")
public class LicenciementEconomiqueController {

    private final LicenciementEconomiqueService service;

    public LicenciementEconomiqueController(LicenciementEconomiqueService service) {
        this.service = service;
    }

    @PostMapping
    public LicenciementEconomiqueResponse calculate(@PathVariable UUID caseFileId,
                                                    @RequestBody LicenciementEconomiqueRequest request,
                                                    @AuthenticationPrincipal OidcUser oidcUser,
                                                    Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public LicenciementEconomiqueResponse get(@PathVariable UUID caseFileId,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
