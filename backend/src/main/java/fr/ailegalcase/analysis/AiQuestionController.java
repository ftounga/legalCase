package fr.ailegalcase.analysis;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/ai-questions")
public class AiQuestionController {

    private final AiQuestionQueryService aiQuestionQueryService;

    public AiQuestionController(AiQuestionQueryService aiQuestionQueryService) {
        this.aiQuestionQueryService = aiQuestionQueryService;
    }

    @GetMapping
    public List<AiQuestionResponse> list(
            @PathVariable UUID caseFileId,
            @RequestParam(required = false) UUID analysisId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        if (analysisId != null) {
            return aiQuestionQueryService.listQuestionsByAnalysisId(caseFileId, analysisId, oidcUser, provider, principal);
        }
        return aiQuestionQueryService.listQuestions(caseFileId, oidcUser, provider, principal);
    }
}
