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
 * SF-212-19 : endpoints REST pour l'analyse de la régularité d'une mise à
 * pied disciplinaire (F-DT-48-mise-a-pied-disciplinaire, FRANCE — L. 1331-1
 * CT ; L. 1332-1 à L. 1332-4 CT ; Cass. soc. constante).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/mise-a-pied-disciplinaire")
public class MiseAPiedDisciplinaireController {

    private final MiseAPiedDisciplinaireService service;

    public MiseAPiedDisciplinaireController(MiseAPiedDisciplinaireService service) {
        this.service = service;
    }

    @PostMapping
    public MiseAPiedDisciplinaireResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody MiseAPiedDisciplinaireRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<MiseAPiedDisciplinaireResponse> get(
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
