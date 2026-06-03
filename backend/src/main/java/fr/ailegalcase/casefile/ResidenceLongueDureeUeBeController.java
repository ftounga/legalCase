package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-221-03 : endpoints REST de l'outil résident longue durée UE BE
 * (Loi 15/12/1980 art. 15bis — directive 2003/109/CE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/residence-longue-duree-ue-be-analysis")
public class ResidenceLongueDureeUeBeController {

    private final ResidenceLongueDureeUeBeService service;

    public ResidenceLongueDureeUeBeController(ResidenceLongueDureeUeBeService service) {
        this.service = service;
    }

    @PostMapping
    public ResidenceLongueDureeUeBeResponse calculate(@PathVariable UUID caseFileId,
                                                      @RequestBody ResidenceLongueDureeUeBeRequest request,
                                                      @AuthenticationPrincipal OidcUser oidcUser,
                                                      Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResidenceLongueDureeUeBeResponse get(@PathVariable UUID caseFileId,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
