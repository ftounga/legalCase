package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-221-01 : endpoints REST de l'outil prorogation de la carte A BE
 * (Loi 15/12/1980 art. 13 + AR 08/10/1981 art. 33 — séjour temporaire / limité).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/carte-a-prorogation-be-analysis")
public class CarteAProrogationBeController {

    private final CarteAProrogationBeService service;

    public CarteAProrogationBeController(CarteAProrogationBeService service) {
        this.service = service;
    }

    @PostMapping
    public CarteAProrogationBeResponse calculate(@PathVariable UUID caseFileId,
                                                 @RequestBody CarteAProrogationBeRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CarteAProrogationBeResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
