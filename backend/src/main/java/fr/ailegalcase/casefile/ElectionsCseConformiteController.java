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
 * SF-212-31 : endpoints REST pour la vérification de conformité du
 * processus électoral CSE (F-DT-65-elections-cse-conformite, FRANCE —
 * L. 2314-1 à L. 2314-37 CT ; R. 2314-1+ CT ; ordonnances Macron 22/09/2017).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/elections-cse-conformite")
public class ElectionsCseConformiteController {

    private final ElectionsCseConformiteService service;

    public ElectionsCseConformiteController(ElectionsCseConformiteService service) {
        this.service = service;
    }

    @PostMapping
    public ElectionsCseConformiteResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody ElectionsCseConformiteRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<ElectionsCseConformiteResponse> get(
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
