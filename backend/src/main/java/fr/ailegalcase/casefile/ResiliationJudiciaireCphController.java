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
 * SF-206-07 : endpoints REST pour l'analyse d'une demande de résiliation
 * judiciaire du contrat de travail aux torts de l'employeur (FR — Cass. soc.
 * 16/03/1989 ; Cass. soc. 20/01/1998 ; art. L.1411-1 CT ; art. 1224 /
 * 1227-1228 C.civ.).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/resiliation-judiciaire-cph")
public class ResiliationJudiciaireCphController {

    private final ResiliationJudiciaireCphService service;

    public ResiliationJudiciaireCphController(ResiliationJudiciaireCphService service) {
        this.service = service;
    }

    @PostMapping
    public ResiliationJudiciaireCphResponse calculate(@PathVariable UUID caseFileId,
                                                      @RequestBody ResiliationJudiciaireCphRequest request,
                                                      @AuthenticationPrincipal OidcUser oidcUser,
                                                      Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResiliationJudiciaireCphResponse get(@PathVariable UUID caseFileId,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
