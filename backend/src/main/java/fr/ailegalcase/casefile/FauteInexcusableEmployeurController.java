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
 * SF-212-09 : endpoints REST pour l'évaluation de la faute inexcusable de
 * l'employeur (F-DT-91, FRANCE — L. 452-1 à L. 452-5 CSS ; Cass. ass. plén.
 * 24/06/2005 ; L. 4121-1 CT).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/faute-inexcusable-employeur")
public class FauteInexcusableEmployeurController {

    private final FauteInexcusableEmployeurService service;

    public FauteInexcusableEmployeurController(FauteInexcusableEmployeurService service) {
        this.service = service;
    }

    @PostMapping
    public FauteInexcusableEmployeurResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody FauteInexcusableEmployeurRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<FauteInexcusableEmployeurResponse> get(
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
