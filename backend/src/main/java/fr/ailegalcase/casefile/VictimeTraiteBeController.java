package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-221-06 : endpoints REST de l'outil titre de séjour victime de la traite des êtres
 * humains BE (art. 61/2 et s. Loi 15/12/1980 ; circulaire du 26/09/2008).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/victime-traite-be-analysis")
public class VictimeTraiteBeController {

    private final VictimeTraiteBeService service;

    public VictimeTraiteBeController(VictimeTraiteBeService service) {
        this.service = service;
    }

    @PostMapping
    public VictimeTraiteBeResponse calculate(@PathVariable UUID caseFileId,
                                             @RequestBody VictimeTraiteBeRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public VictimeTraiteBeResponse get(@PathVariable UUID caseFileId,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
