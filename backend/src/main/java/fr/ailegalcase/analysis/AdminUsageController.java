package fr.ailegalcase.analysis;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/admin/usage")
public class AdminUsageController {

    private final AdminUsageService adminUsageService;

    public AdminUsageController(AdminUsageService adminUsageService) {
        this.adminUsageService = adminUsageService;
    }

    @GetMapping
    public WorkspaceUsageSummaryResponse getSummary(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return adminUsageService.getWorkspaceSummary(oidcUser, principal);
    }
}
