package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-29 : endpoints POST/GET pour l'analyse de conformité de la négociation
 * annuelle obligatoire (NAO, art. L.2242-1 à L.2242-8 CT, F-DT-66). Outil
 * single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/nao-negociation-annuelle-analysis")
public class NaoNegociationAnnuelleController {

    private final NaoNegociationAnnuelleService service;

    public NaoNegociationAnnuelleController(NaoNegociationAnnuelleService service) {
        this.service = service;
    }

    @PostMapping
    public NaoNegociationAnnuelleResponse analyze(@PathVariable UUID caseFileId,
                                                  @RequestBody NaoNegociationAnnuelleRequest request,
                                                  @AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public NaoNegociationAnnuelleResponse get(@PathVariable UUID caseFileId,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
