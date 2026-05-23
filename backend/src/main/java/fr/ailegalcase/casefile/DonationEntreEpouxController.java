package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-23 : endpoint REST pour l'outil Donation entre époux FR
 * (art. 1091-1100 Cciv). POST/GET
 * /api/v1/case-files/{caseFileId}/donation-entre-epoux.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/donation-entre-epoux")
public class DonationEntreEpouxController {

    private final DonationEntreEpouxService service;

    public DonationEntreEpouxController(DonationEntreEpouxService service) {
        this.service = service;
    }

    @PostMapping
    public DonationEntreEpouxResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody DonationEntreEpouxRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DonationEntreEpouxResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
