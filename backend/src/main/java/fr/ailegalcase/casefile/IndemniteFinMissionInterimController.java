package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-DT-18-01 : endpoint REST de l'outil "Indemnité de fin de mission intérim"
 * (art. L.1251-32 Code du travail). Pattern miroir de F-DT-17 cdd-indemnite-precarite.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/fin-mission-interim")
public class IndemniteFinMissionInterimController {

    private final IndemniteFinMissionInterimService service;

    public IndemniteFinMissionInterimController(IndemniteFinMissionInterimService service) {
        this.service = service;
    }

    @PostMapping
    public IndemniteFinMissionInterimResponse calculate(@PathVariable UUID caseFileId,
                                                        @RequestBody IndemniteFinMissionInterimRequest request,
                                                        @AuthenticationPrincipal OidcUser oidcUser,
                                                        Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public IndemniteFinMissionInterimResponse get(@PathVariable UUID caseFileId,
                                                  @AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
