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
 * SF-212-15 : endpoints REST pour l'analyse de la conformité du dispositif de
 * télétravail et des litiges courants (F-DT-82-teletravail-accord,
 * FRANCE — L. 1222-9 à L. 1222-11 CT ; ANI télétravail 26/11/2020).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/teletravail-accord")
public class TeletravailAccordController {

    private final TeletravailAccordService service;

    public TeletravailAccordController(TeletravailAccordService service) {
        this.service = service;
    }

    @PostMapping
    public TeletravailAccordResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody TeletravailAccordRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<TeletravailAccordResponse> get(
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
