package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-220-06 : endpoints POST/GET pour l'analyse de contestation / radiation d'un
 * signalement SIS aux fins de non-admission (Règl. UE 2018/1860 / CESEDA
 * L.312-3, F-IM-52-signalement-sis-fr). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/signalement-sis-analysis")
public class SignalementSisController {

    private final SignalementSisService service;

    public SignalementSisController(SignalementSisService service) {
        this.service = service;
    }

    @PostMapping
    public SignalementSisResponse analyze(@PathVariable UUID caseFileId,
                                          @RequestBody SignalementSisRequest request,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public SignalementSisResponse get(@PathVariable UUID caseFileId,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
