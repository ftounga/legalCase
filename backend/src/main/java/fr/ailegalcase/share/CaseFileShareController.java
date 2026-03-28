package fr.ailegalcase.share;

import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
public class CaseFileShareController {

    private final CaseFileShareService shareService;

    public CaseFileShareController(CaseFileShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping("/api/v1/case-files/{id}/shares")
    @ResponseStatus(HttpStatus.CREATED)
    public ShareResponse createShare(@PathVariable UUID id,
                                     @Valid @RequestBody CreateShareRequest request,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     Principal principal) {
        return shareService.createShare(id, request.expiresInDays(), oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    @GetMapping("/api/v1/case-files/{id}/shares")
    public List<ShareResponse> listShares(@PathVariable UUID id,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return shareService.listActiveShares(id, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    @DeleteMapping("/api/v1/case-files/{id}/shares/{shareId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeShare(@PathVariable UUID id,
                            @PathVariable UUID shareId,
                            @AuthenticationPrincipal OidcUser oidcUser,
                            Principal principal) {
        shareService.revokeShare(id, shareId, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    @GetMapping("/api/v1/public/shares/{token}")
    public PublicShareResponse getPublicShare(@PathVariable String token) {
        return shareService.getPublicShare(token);
    }
}
