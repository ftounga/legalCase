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
 * SF-DT-36-01 : endpoints REST pour l'analyse des nullités de procédure de
 * licenciement (vices de forme côté employeur — FRANCE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/procedure-nullite-licenciement")
public class ProcedureNulliteLicenciementController {

    private final ProcedureNulliteLicenciementService service;

    public ProcedureNulliteLicenciementController(ProcedureNulliteLicenciementService service) {
        this.service = service;
    }

    @PostMapping
    public ProcedureNulliteLicenciementResponse calculate(@PathVariable UUID caseFileId,
                                                          @RequestBody ProcedureNulliteLicenciementRequest request,
                                                          @AuthenticationPrincipal OidcUser oidcUser,
                                                          Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ProcedureNulliteLicenciementResponse get(@PathVariable UUID caseFileId,
                                                    @AuthenticationPrincipal OidcUser oidcUser,
                                                    Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
