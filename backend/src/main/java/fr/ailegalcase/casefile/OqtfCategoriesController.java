package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-09 : endpoints POST/GET pour l'analyse de la catégorie d'OQTF L. 611-1
 * CESEDA (1° à 7°) et les moyens de défense spécifiques. Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/oqtf-categories-analysis")
public class OqtfCategoriesController {

    private final OqtfCategoriesService service;

    public OqtfCategoriesController(OqtfCategoriesService service) {
        this.service = service;
    }

    @PostMapping
    public OqtfCategoriesResponse analyze(@PathVariable UUID caseFileId,
                                          @RequestBody OqtfCategoriesRequest request,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public OqtfCategoriesResponse get(@PathVariable UUID caseFileId,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
