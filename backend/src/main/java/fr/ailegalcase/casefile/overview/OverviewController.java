package fr.ailegalcase.casefile.overview;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * F-289 / SF-289-01 — « Vue d'ensemble » d'un dossier (lecture seule).
 * Agrège pilotage + attention + fil chronologique en {@code fail-open} par source.
 * Isolation workspace via le dossier (mirror {@code EcheancierController}).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/overview")
public class OverviewController {

    private final OverviewService overviewService;

    public OverviewController(OverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping
    public OverviewResponse get(@PathVariable UUID caseFileId,
                                @AuthenticationPrincipal OidcUser oidcUser,
                                Principal principal) {
        return overviewService.overview(caseFileId, oidcUser, principal);
    }
}
