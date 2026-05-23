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
 * SF-212-05 : endpoints REST pour l'analyse de transfert d'entreprise
 * au titre de l'art. L. 1224-1 CT (FRANCE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/transfert-entreprise-l1224-1")
public class TransfertEntrepriseL12241Controller {

    private final TransfertEntrepriseL12241Service service;

    public TransfertEntrepriseL12241Controller(TransfertEntrepriseL12241Service service) {
        this.service = service;
    }

    @PostMapping
    public TransfertEntrepriseL12241Response calculate(
            @PathVariable UUID caseFileId,
            @RequestBody TransfertEntrepriseL12241Request request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<TransfertEntrepriseL12241Response> get(
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
