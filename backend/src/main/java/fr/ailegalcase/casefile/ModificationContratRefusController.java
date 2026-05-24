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
 * SF-212-11 : endpoints REST pour l'analyse de la modification du contrat
 * refusée par le salarié (F-DT-70, FRANCE — Cass. soc. ; L. 1222-6 CT).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/modification-contrat-refus")
public class ModificationContratRefusController {

    private final ModificationContratRefusService service;

    public ModificationContratRefusController(ModificationContratRefusService service) {
        this.service = service;
    }

    @PostMapping
    public ModificationContratRefusResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody ModificationContratRefusRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<ModificationContratRefusResponse> get(
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
