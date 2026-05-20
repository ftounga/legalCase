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
 * SF-DT-38-01 : endpoints REST pour la qualification d'une rupture pendant la
 * période d'essai (FRANCE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/rupture-periode-essai")
public class RupturePeriodeEssaiController {

    private final RupturePeriodeEssaiService service;

    public RupturePeriodeEssaiController(RupturePeriodeEssaiService service) {
        this.service = service;
    }

    @PostMapping
    public RupturePeriodeEssaiResponse calculate(@PathVariable UUID caseFileId,
                                                 @RequestBody RupturePeriodeEssaiRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RupturePeriodeEssaiResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
