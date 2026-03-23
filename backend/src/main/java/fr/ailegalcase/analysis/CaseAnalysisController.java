package fr.ailegalcase.analysis;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/analyze")
public class CaseAnalysisController {

    private final CaseAnalysisCommandService caseAnalysisCommandService;

    public CaseAnalysisController(CaseAnalysisCommandService caseAnalysisCommandService) {
        this.caseAnalysisCommandService = caseAnalysisCommandService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void analyze(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        caseAnalysisCommandService.triggerCaseAnalysis(caseFileId, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }
}
