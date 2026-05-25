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
 * SF-212-27 : endpoints REST pour l'évaluation des chances de
 * reconnaissance du burn-out comme maladie professionnelle hors tableau
 * (F-DT-64-burnout-reconnaissance-mp, FRANCE — L. 461-1 al. 4 et 5 CSS).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/burnout-reconnaissance-mp")
public class BurnoutReconnaissanceMpController {

    private final BurnoutReconnaissanceMpService service;

    public BurnoutReconnaissanceMpController(BurnoutReconnaissanceMpService service) {
        this.service = service;
    }

    @PostMapping
    public BurnoutReconnaissanceMpResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody BurnoutReconnaissanceMpRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<BurnoutReconnaissanceMpResponse> get(
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
