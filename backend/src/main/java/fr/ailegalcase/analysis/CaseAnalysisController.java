package fr.ailegalcase.analysis;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/case-analysis")
public class CaseAnalysisController {

    private final CaseAnalysisQueryService caseAnalysisQueryService;

    public CaseAnalysisController(CaseAnalysisQueryService caseAnalysisQueryService) {
        this.caseAnalysisQueryService = caseAnalysisQueryService;
    }

    @GetMapping
    public CaseAnalysisResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return caseAnalysisQueryService.getAnalysis(caseFileId, oidcUser, OAuthProviderResolver.resolve(principal));
    }
}
