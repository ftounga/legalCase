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
 * SF-212-33 : endpoints REST pour l'analyse de requalification d'un
 * contrat à temps partiel en temps complet
 * (F-DT-49-temps-partiel-requalification, FRANCE — L. 3123-1 à
 * L. 3123-20 CT, L. 3245-1 CT prescription rappel salaire).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/temps-partiel-requalification")
public class TempsPartielRequalificationController {

    private final TempsPartielRequalificationService service;

    public TempsPartielRequalificationController(TempsPartielRequalificationService service) {
        this.service = service;
    }

    @PostMapping
    public TempsPartielRequalificationResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody TempsPartielRequalificationRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<TempsPartielRequalificationResponse> get(
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
