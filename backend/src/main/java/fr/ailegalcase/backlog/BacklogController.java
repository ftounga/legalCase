package fr.ailegalcase.backlog;

import fr.ailegalcase.backlog.BacklogDtos.*;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.superadmin.SuperAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/super-admin/backlog")
public class BacklogController {

    private final BacklogSyncService syncService;
    private final BacklogQueryService queryService;
    private final SuperAdminService superAdminService;

    public BacklogController(BacklogSyncService syncService,
                             BacklogQueryService queryService,
                             SuperAdminService superAdminService) {
        this.syncService = syncService;
        this.queryService = queryService;
        this.superAdminService = superAdminService;
    }

    @PostMapping("/sync")
    public BacklogSyncResult sync(@AuthenticationPrincipal OidcUser oidcUser, Principal principal) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return syncService.sync(SyncTrigger.MANUAL);
    }

    @GetMapping("/features")
    public Page<BacklogFeatureSummary> listFeatures(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @RequestParam(required = false) BacklogStatus status,
            @RequestParam(required = false) BacklogDomain domain,
            @RequestParam(required = false) BacklogPriority priority,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50, sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return queryService.searchFeatures(status, domain, priority, search, pageable);
    }

    @GetMapping("/features/{code}")
    public BacklogFeatureDetail getFeature(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @PathVariable String code) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return queryService.getFeatureDetail(code);
    }

    @GetMapping("/marketing-tasks")
    public Page<BacklogMarketingTaskSummary> listMarketingTasks(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @RequestParam(required = false) BacklogMarketingStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50, sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return queryService.searchMarketingTasks(status, search, pageable);
    }

    @GetMapping("/sync-runs")
    public List<BacklogSyncRunSummary> listSyncRuns(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @RequestParam(defaultValue = "10") int limit) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return queryService.recentSyncRuns(limit);
    }

    @GetMapping("/freshness")
    public BacklogFreshness getFreshness(@AuthenticationPrincipal OidcUser oidcUser, Principal principal) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return queryService.getFreshness();
    }
}
