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
 * SF-212-25 : endpoints REST pour l'évaluation de la protection du
 * lanceur d'alerte (F-DT-61, FRANCE — L. 1132-3-3 CT ; loi Sapin II ;
 * loi Waserman 2022).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/lanceur-alerte-protection")
public class LanceurAlerteProtectionController {

    private final LanceurAlerteProtectionService service;

    public LanceurAlerteProtectionController(LanceurAlerteProtectionService service) {
        this.service = service;
    }

    @PostMapping
    public LanceurAlerteProtectionResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody LanceurAlerteProtectionRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<LanceurAlerteProtectionResponse> get(
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
