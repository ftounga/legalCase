package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-IM-19-01 : controller HTTP pour l'analyse d'éligibilité mineur étranger
 * (MNA, L.435-3, DCEM, TIR — France).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/mineurs-immigration-analysis")
public class MineursImmigrationController {

    private final MineursImmigrationService service;

    public MineursImmigrationController(MineursImmigrationService service) {
        this.service = service;
    }

    @PostMapping
    public MineursImmigrationResponse calculate(@PathVariable UUID caseFileId,
                                                @RequestBody MineursImmigrationRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public MineursImmigrationResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
