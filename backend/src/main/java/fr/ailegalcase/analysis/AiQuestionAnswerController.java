package fr.ailegalcase.analysis;

import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai-questions/{questionId}/answer")
public class AiQuestionAnswerController {

    private final AiQuestionAnswerCommandService commandService;

    public AiQuestionAnswerController(AiQuestionAnswerCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void answer(
            @PathVariable UUID questionId,
            @Valid @RequestBody AiQuestionAnswerRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        commandService.answer(questionId, request.answerText(), oidcUser, OAuthProviderResolver.resolve(principal));
    }
}
