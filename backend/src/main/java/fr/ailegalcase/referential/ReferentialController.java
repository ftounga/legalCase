package fr.ailegalcase.referential;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referentials")
public class ReferentialController {

    private static final Set<String> VALID_DOMAINS = Set.of(
            "DROIT_DU_TRAVAIL", "DROIT_IMMIGRATION", "DROIT_FAMILLE");

    private final LegalReferentialService referentialService;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public ReferentialController(LegalReferentialService referentialService,
                                 CurrentUserResolver currentUserResolver,
                                 WorkspaceMemberRepository workspaceMemberRepository) {
        this.referentialService = referentialService;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @GetMapping
    public ResponseEntity<ReferentialResponse> getReferentials(
            @RequestParam String domain,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {

        if (domain == null || !VALID_DOMAINS.contains(domain.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Paramètre 'domain' invalide. Valeurs acceptées : " + VALID_DOMAINS);
        }

        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        UUID workspaceId = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId())
                .orElse(null);

        return ResponseEntity.ok(referentialService.getReferentials(domain.toUpperCase(), workspaceId));
    }
}
