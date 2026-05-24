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
 * SF-212-17 : endpoints REST pour l'analyse de la rupture anticipée d'un CDD
 * (F-DT-43-rupture-anticipee-cdd, FRANCE — L. 1243-1 à L. 1243-4 CT ;
 * L. 1243-8 CT ; Cass. soc. constante).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/rupture-anticipee-cdd")
public class RuptureAnticipeeCddController {

    private final RuptureAnticipeeCddService service;

    public RuptureAnticipeeCddController(RuptureAnticipeeCddService service) {
        this.service = service;
    }

    @PostMapping
    public RuptureAnticipeeCddResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody RuptureAnticipeeCddRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<RuptureAnticipeeCddResponse> get(
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
