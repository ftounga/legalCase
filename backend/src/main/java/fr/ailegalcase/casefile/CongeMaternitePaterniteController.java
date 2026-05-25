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
 * SF-212-29 : endpoints REST de l'outil congé maternité / paternité
 * (F-DT-77-conge-paternite-maternite, FRANCE — L. 1225-1 à L. 1225-40 CT ;
 * L. 331-3 CSS ; loi du 16/03/2021).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/conge-maternite-paternite")
public class CongeMaternitePaterniteController {

    private final CongeMaternitePaterniteService service;

    public CongeMaternitePaterniteController(CongeMaternitePaterniteService service) {
        this.service = service;
    }

    @PostMapping
    public CongeMaternitePaterniteResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody CongeMaternitePaterniteRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<CongeMaternitePaterniteResponse> get(
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
