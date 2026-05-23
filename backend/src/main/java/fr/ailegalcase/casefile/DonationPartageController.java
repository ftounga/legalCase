package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-216-29 : endpoint REST pour l'outil Donation-partage FR (art. 1075 à
 * 1075-5 Cciv). POST/GET /api/v1/case-files/{caseFileId}/donation-partage.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/donation-partage")
public class DonationPartageController {

    private final DonationPartageService service;

    public DonationPartageController(DonationPartageService service) {
        this.service = service;
    }

    @PostMapping
    public DonationPartageResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody DonationPartageRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DonationPartageResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
