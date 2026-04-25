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
 * SF-FA-23-01 : endpoints REST pour l'outil "Ordonnance sur requête"
 * (mesures urgentes familiales — art. 493 CPC FR / 1025 CJ BE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/ordonnance-requete-analysis")
public class OrdonnanceRequeteController {

    private final OrdonnanceRequeteService service;

    public OrdonnanceRequeteController(OrdonnanceRequeteService service) {
        this.service = service;
    }

    @PostMapping
    public OrdonnanceRequeteResponse calculate(@PathVariable UUID caseFileId,
                                                @RequestBody OrdonnanceRequeteRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public OrdonnanceRequeteResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
