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
 * SF-206-03 : endpoints REST pour le chiffrage du rappel de congés payés
 * acquis pendant un arrêt maladie (FR — loi 22/04/2024).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/conges-payes-arret-maladie")
public class CongesPayesArretMaladieController {

    private final CongesPayesArretMaladieService service;

    public CongesPayesArretMaladieController(CongesPayesArretMaladieService service) {
        this.service = service;
    }

    @PostMapping
    public CongesPayesArretMaladieResponse calculate(@PathVariable UUID caseFileId,
                                                     @RequestBody CongesPayesArretMaladieRequest request,
                                                     @AuthenticationPrincipal OidcUser oidcUser,
                                                     Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CongesPayesArretMaladieResponse get(@PathVariable UUID caseFileId,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
