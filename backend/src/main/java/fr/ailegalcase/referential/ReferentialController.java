package fr.ailegalcase.referential;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMember;
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

    private static final Set<String> OWNER_ADMIN_ROLES = Set.of("OWNER", "ADMIN");

    private final LegalReferentialService referentialService;
    private final ReferentialValidationService validationService;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public ReferentialController(LegalReferentialService referentialService,
                                 ReferentialValidationService validationService,
                                 CurrentUserResolver currentUserResolver,
                                 WorkspaceMemberRepository workspaceMemberRepository) {
        this.referentialService = referentialService;
        this.validationService = validationService;
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
        WorkspaceMember member = workspaceMemberRepository.findByUserAndPrimaryTrue(user).orElse(null);
        UUID workspaceId = member != null ? member.getWorkspace().getId() : null;
        // SF-137-01 : filtrer les entries sur le country du workspace pour ne pas
        // polluer l'écran Guides & barèmes avec des entries de l'autre pays.
        String workspaceCountry = member != null ? member.getWorkspace().getCountry() : null;

        return ResponseEntity.ok(referentialService.getReferentials(domain.toUpperCase(), workspaceId, workspaceCountry));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReferentialUpdateResponse> updateReferential(
            @PathVariable UUID id,
            @RequestBody ReferentialUpdateRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {

        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        WorkspaceMember member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace introuvable"));

        if (!OWNER_ADMIN_ROLES.contains(member.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seuls les OWNER et ADMIN peuvent modifier les référentiels.");
        }

        UUID workspaceId = member.getWorkspace().getId();
        UUID userId = user.getId();

        ReferentialUpdateResponse response = referentialService.updateReferential(
                id, workspaceId, userId,
                request.label(), request.valueJson(), request.force(),
                validationService);

        return ResponseEntity.ok(response);
    }
}
