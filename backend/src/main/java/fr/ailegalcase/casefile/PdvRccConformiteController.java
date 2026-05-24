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
 * SF-212-35 : endpoints REST pour l'analyse de la conformité d'un PDV ou
 * d'une RCC (F-DT-46-pdv-rcc-conformite, FRANCE — L. 1237-17 à L. 1237-19-14
 * CT).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/pdv-rcc-conformite")
public class PdvRccConformiteController {

    private final PdvRccConformiteService service;

    public PdvRccConformiteController(PdvRccConformiteService service) {
        this.service = service;
    }

    @PostMapping
    public PdvRccConformiteResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody PdvRccConformiteRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<PdvRccConformiteResponse> get(
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
