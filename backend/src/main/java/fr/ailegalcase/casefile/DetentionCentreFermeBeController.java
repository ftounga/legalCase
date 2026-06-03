package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-221-04 : endpoints REST de l'outil détention en centre fermé + requête de mise en
 * liberté BE (Loi 15/12/1980 art. 7 al. 3 / 27 / 29 / 74/5 ; AR 02/08/2002 ;
 * chambre du conseil art. 71 et s.).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/detention-centre-ferme-be-analysis")
public class DetentionCentreFermeBeController {

    private final DetentionCentreFermeBeService service;

    public DetentionCentreFermeBeController(DetentionCentreFermeBeService service) {
        this.service = service;
    }

    @PostMapping
    public DetentionCentreFermeBeResponse calculate(@PathVariable UUID caseFileId,
                                                    @RequestBody DetentionCentreFermeBeRequest request,
                                                    @AuthenticationPrincipal OidcUser oidcUser,
                                                    Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DetentionCentreFermeBeResponse get(@PathVariable UUID caseFileId,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
