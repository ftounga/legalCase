package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * F-283 / SF-283-02 — endpoint de lecture de la « vague de pièces » d'un dossier
 * (pièces ajoutées depuis la dernière analyse). Lecture seule, isolation workspace
 * via le dossier (cf. {@link PiecesWaveService}).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/pieces-wave")
public class PiecesWaveController {

    private final PiecesWaveService service;

    public PiecesWaveController(PiecesWaveService service) {
        this.service = service;
    }

    @GetMapping
    public PiecesWaveResponse wave(@PathVariable UUID caseFileId,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return service.wave(caseFileId, oidcUser, principal);
    }
}
