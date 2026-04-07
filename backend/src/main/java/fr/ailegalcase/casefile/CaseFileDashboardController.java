package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/dashboard")
public class CaseFileDashboardController {

    private final CaseFileDashboardService dashboardService;

    public CaseFileDashboardController(CaseFileDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public CaseFileDashboardResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return dashboardService.getDashboard(caseFileId, oidcUser, principal);
    }
}
