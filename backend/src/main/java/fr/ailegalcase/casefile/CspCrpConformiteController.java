package fr.ailegalcase.casefile;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-212-07 : endpoints REST pour l'analyse de conformité de la proposition
 * CSP/CRP (FRANCE — L. 1233-65 à L. 1233-70 CT ; ANI CSP 19/07/2011 ; DARES).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/csp-crp-conformite")
public class CspCrpConformiteController {

    private final CspCrpConformiteService service;

    public CspCrpConformiteController(CspCrpConformiteService service) {
        this.service = service;
    }

    @PostMapping
    public CspCrpConformiteResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody CspCrpConformiteRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<CspCrpConformiteResponse> get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        try {
            return ResponseEntity.ok(service.get(caseFileId, oidcUser, principal));
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return ResponseEntity.noContent().build();
            }
            throw e;
        }
    }
}
