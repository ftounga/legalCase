package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-31 : endpoints POST/GET pour l'analyse de validité d'un accord
 * d'entreprise au regard des conditions de majorité (art. L.2232-12 CT ;
 * L.2261-7 et s. CT, F-DT-67). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/accord-entreprise-validite-analysis")
public class AccordEntrepriseValiditeController {

    private final AccordEntrepriseValiditeService service;

    public AccordEntrepriseValiditeController(AccordEntrepriseValiditeService service) {
        this.service = service;
    }

    @PostMapping
    public AccordEntrepriseValiditeResponse analyze(@PathVariable UUID caseFileId,
                                                    @RequestBody AccordEntrepriseValiditeRequest request,
                                                    @AuthenticationPrincipal OidcUser oidcUser,
                                                    Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AccordEntrepriseValiditeResponse get(@PathVariable UUID caseFileId,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
