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
@RequestMapping("/api/v1/case-files/{caseFileId}/case-analysis")
public class CaseAnalysisReadController {

    private final CaseAnalysisQueryService caseAnalysisQueryService;

    public CaseAnalysisReadController(CaseAnalysisQueryService caseAnalysisQueryService) {
        this.caseAnalysisQueryService = caseAnalysisQueryService;
    }

    @GetMapping
    public CaseAnalysisResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return caseAnalysisQueryService.getAnalysis(caseFileId, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    @GetMapping("/versions")
    public List<CaseAnalysisResponse.VersionSummary> listVersions(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return caseAnalysisQueryService.listVersions(caseFileId, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    @GetMapping("/versions/{version}")
    public CaseAnalysisResponse getByVersion(
            @PathVariable UUID caseFileId,
            @PathVariable int version,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return caseAnalysisQueryService.getByVersion(caseFileId, version, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }
}
