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
 * SF-212-21 : endpoints REST pour l'analyse de la validité d'une démission au
 * regard de la jurisprudence sur la volonté claire et non équivoque
 * (F-DT-41-demission-validite-equivoque, FRANCE — L. 1237-1 CT ; Cass. soc.
 * 09/05/2007).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/demission-validite-equivoque")
public class DemissionEquivoqueController {

    private final DemissionEquivoqueService service;

    public DemissionEquivoqueController(DemissionEquivoqueService service) {
        this.service = service;
    }

    @PostMapping
    public DemissionEquivoqueResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody DemissionEquivoqueRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ResponseEntity<DemissionEquivoqueResponse> get(
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
