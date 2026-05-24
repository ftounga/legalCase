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
 * SF-212-23 : endpoints REST pour l'analyse d'une discrimination salariale
 * fondée sur le sexe (F-DT-56-egalite-salariale-femmes-hommes, FRANCE —
 * L. 1142-7 à L. 1142-10 CT ; L. 1144-1 CT ; L. 3221-2 CT ; loi 05/09/2018).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/egalite-salariale-fh")
public class EgaliteSalarialeFhController {

    private final EgaliteSalarialeFhService service;

    public EgaliteSalarialeFhController(EgaliteSalarialeFhService service) {
        this.service = service;
    }

    @PostMapping
    public EgaliteSalarialeFhResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody EgaliteSalarialeFhRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<EgaliteSalarialeFhResponse> get(
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
