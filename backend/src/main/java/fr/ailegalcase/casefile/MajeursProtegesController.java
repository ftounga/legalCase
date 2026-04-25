package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-25-01 : endpoint REST mesures de protection des majeurs
 * (art. 433-441 + 494-1 et s. Cciv).
 * Single-country FR DROIT_FAMILLE.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/majeurs-proteges")
public class MajeursProtegesController {

    private final MajeursProtegesService service;

    public MajeursProtegesController(MajeursProtegesService service) {
        this.service = service;
    }

    @PostMapping
    public MajeursProtegesResponse calculate(@PathVariable UUID caseFileId,
                                             @RequestBody MajeursProtegesRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public MajeursProtegesResponse get(@PathVariable UUID caseFileId,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
