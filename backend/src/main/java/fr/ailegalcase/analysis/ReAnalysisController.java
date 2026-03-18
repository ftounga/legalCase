package fr.ailegalcase.analysis;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/re-analyze")
public class ReAnalysisController {

    private final ReAnalysisCommandService reAnalysisCommandService;

    public ReAnalysisController(ReAnalysisCommandService reAnalysisCommandService) {
        this.reAnalysisCommandService = reAnalysisCommandService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reAnalyze(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        reAnalysisCommandService.triggerReAnalysis(caseFileId, oidcUser, OAuthProviderResolver.resolve(principal));
    }
}
