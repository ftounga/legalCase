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
 * SF-212-37 : endpoints REST pour la préparation de la conciliation BCO
 * du Conseil de Prud'hommes (F-DT-84-conciliation-cph-bca, FRANCE —
 * R. 1454-7 à R. 1454-12 CT ; L. 1235-1 al. 3 CT — barème transactions BCA).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/conciliation-cph-bca")
public class ConciliationCphBcaController {

    private final ConciliationCphBcaService service;

    public ConciliationCphBcaController(ConciliationCphBcaService service) {
        this.service = service;
    }

    @PostMapping
    public ConciliationCphBcaResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody ConciliationCphBcaRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<ConciliationCphBcaResponse> get(
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
