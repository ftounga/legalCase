package fr.ailegalcase.analysis;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/analysis-jobs")
public class AnalysisJobController {

    private final AnalysisJobQueryService analysisJobQueryService;

    public AnalysisJobController(AnalysisJobQueryService analysisJobQueryService) {
        this.analysisJobQueryService = analysisJobQueryService;
    }

    @GetMapping
    public List<AnalysisJobResponse> list(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return analysisJobQueryService.listJobs(caseFileId, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }
}
