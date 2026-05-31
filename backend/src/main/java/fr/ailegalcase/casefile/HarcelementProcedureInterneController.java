package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-27 : endpoints POST/GET pour l'analyse de conformité de la procédure
 * interne de traitement d'un signalement de harcèlement (art. L.1153-5-1,
 * L.2314-1, L.1152-4, L.4121-1 CT, F-DT-59). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/harcelement-procedure-interne-analysis")
public class HarcelementProcedureInterneController {

    private final HarcelementProcedureInterneService service;

    public HarcelementProcedureInterneController(HarcelementProcedureInterneService service) {
        this.service = service;
    }

    @PostMapping
    public HarcelementProcedureInterneResponse analyze(@PathVariable UUID caseFileId,
                                                       @RequestBody HarcelementProcedureInterneRequest request,
                                                       @AuthenticationPrincipal OidcUser oidcUser,
                                                       Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public HarcelementProcedureInterneResponse get(@PathVariable UUID caseFileId,
                                                   @AuthenticationPrincipal OidcUser oidcUser,
                                                   Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
