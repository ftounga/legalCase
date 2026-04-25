package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-DT-20-01 : controller exposant l'outil "Rappel de salaire FR"
 * (art. L.3242-1 + L.3245-1 + L.3141-26 Code du travail).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/rappel-salaire")
public class RappelSalaireController {

    private final RappelSalaireService service;

    public RappelSalaireController(RappelSalaireService service) {
        this.service = service;
    }

    @PostMapping
    public RappelSalaireResponse calculate(@PathVariable UUID caseFileId,
                                           @RequestBody RappelSalaireRequest request,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RappelSalaireResponse get(@PathVariable UUID caseFileId,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
