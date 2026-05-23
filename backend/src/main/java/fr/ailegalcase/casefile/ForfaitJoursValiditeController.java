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
 * SF-212-03 : endpoints REST pour l'analyse de validité du forfait jours
 * (FRANCE — L. 3121-58 à L. 3121-66 CT ; Cass. soc. 29/06/2011 n°09-71.107).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/forfait-jours-validite")
public class ForfaitJoursValiditeController {

    private final ForfaitJoursValiditeService service;

    public ForfaitJoursValiditeController(ForfaitJoursValiditeService service) {
        this.service = service;
    }

    @PostMapping
    public ForfaitJoursValiditeResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody ForfaitJoursValiditeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<ForfaitJoursValiditeResponse> get(
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
