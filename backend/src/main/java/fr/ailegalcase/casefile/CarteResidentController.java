package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-23 : endpoints POST/GET pour l'analyse d'éligibilité à la carte de
 * résident 10 ans L. 426-1 CESEDA. Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/carte-resident-analysis")
public class CarteResidentController {

    private final CarteResidentService service;

    public CarteResidentController(CarteResidentService service) {
        this.service = service;
    }

    @PostMapping
    public CarteResidentResponse analyze(@PathVariable UUID caseFileId,
                                         @RequestBody CarteResidentRequest request,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CarteResidentResponse get(@PathVariable UUID caseFileId,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
