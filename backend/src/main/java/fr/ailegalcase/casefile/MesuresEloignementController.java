package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-IM-20-01 : controller HTTP pour l'analyse de mesures d'éloignement administratives FR
 * (Expulsion / IRTF / IAT — distinctes de l'OQTF).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/mesures-eloignement-analysis")
public class MesuresEloignementController {

    private final MesuresEloignementService service;

    public MesuresEloignementController(MesuresEloignementService service) {
        this.service = service;
    }

    @PostMapping
    public MesuresEloignementResponse calculate(@PathVariable UUID caseFileId,
                                                @RequestBody MesuresEloignementRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public MesuresEloignementResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
