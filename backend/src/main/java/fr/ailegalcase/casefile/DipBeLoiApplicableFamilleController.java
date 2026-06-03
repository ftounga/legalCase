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
 * SF-223-07 : endpoints REST pour la détermination de la loi applicable en droit
 * de la famille BE (DIP — Rome III ; Règl. UE 2016/1103 ; Règl. UE 650/2012 — à
 * vérifier par avocat belge).
 * POST/GET /api/v1/case-files/{caseFileId}/dip-be-loi-applicable-famille-analysis
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/dip-be-loi-applicable-famille-analysis")
public class DipBeLoiApplicableFamilleController {

    private final DipBeLoiApplicableFamilleService service;

    public DipBeLoiApplicableFamilleController(DipBeLoiApplicableFamilleService service) {
        this.service = service;
    }

    @PostMapping
    public DipBeLoiApplicableFamilleResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody DipBeLoiApplicableFamilleRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DipBeLoiApplicableFamilleResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
