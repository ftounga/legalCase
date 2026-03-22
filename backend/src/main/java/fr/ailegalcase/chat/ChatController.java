package fr.ailegalcase.chat;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@Profile("local")
@RequestMapping("/api/v1/case-files/{caseFileId}/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatMessageResponse sendMessage(
            @PathVariable UUID caseFileId,
            @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return chatService.sendMessage(caseFileId, request, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    @GetMapping
    public List<ChatMessageResponse> getHistory(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return chatService.getHistory(caseFileId, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }
}
