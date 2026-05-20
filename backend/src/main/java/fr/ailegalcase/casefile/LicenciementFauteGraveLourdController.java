package fr.ailegalcase.casefile;

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
import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-212-01 : endpoints REST pour la qualification de la faute disciplinaire
 * (faute grave / faute lourde — FRANCE — L. 1234-1 CT ; L. 1234-9 CT).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/licenciement-faute-grave-lourde")
public class LicenciementFauteGraveLourdController {

    private final LicenciementFauteGraveLourdService service;

    public LicenciementFauteGraveLourdController(LicenciementFauteGraveLourdService service) {
        this.service = service;
    }

    @PostMapping
    public LicenciementFauteGraveLourdResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody LicenciementFauteGraveLourdRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<LicenciementFauteGraveLourdResponse> get(
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
