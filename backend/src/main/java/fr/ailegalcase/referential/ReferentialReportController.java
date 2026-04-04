package fr.ailegalcase.referential;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referentials")
public class ReferentialReportController {

    private final ReferentialReportService reportService;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public ReferentialReportController(ReferentialReportService reportService,
                                       CurrentUserResolver currentUserResolver,
                                       WorkspaceMemberRepository workspaceMemberRepository) {
        this.reportService = reportService;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @PostMapping("/{entryId}/reports")
    public ResponseEntity<Void> report(
            @PathVariable UUID entryId,
            @Valid @RequestBody ReferentialReportRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {

        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        WorkspaceMember member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace introuvable"));

        // Only MEMBER can report; OWNER and ADMIN can modify directly
        if (!"MEMBER".equals(member.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seuls les MEMBER peuvent signaler une anomalie.");
        }

        reportService.report(entryId, member.getWorkspace().getId(), user, request.comment());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
