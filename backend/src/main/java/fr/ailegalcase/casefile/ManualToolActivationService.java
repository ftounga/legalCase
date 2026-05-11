package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * SF-238-03 : service de gestion des activations manuelles d'outils décisionnels.
 *
 * <p>Pattern aligné sur {@link DecisionToolVisibilityService} :</p>
 * <ul>
 *   <li>{@link CurrentUserResolver} résout l'utilisateur authentifié</li>
 *   <li>{@link WorkspaceMemberRepository} vérifie l'isolation workspace</li>
 * </ul>
 */
@Service
public class ManualToolActivationService {

    private final ManualToolActivationRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public ManualToolActivationService(ManualToolActivationRepository repository,
                                       CaseFileRepository caseFileRepository,
                                       WorkspaceMemberRepository workspaceMemberRepository,
                                       CurrentUserResolver currentUserResolver) {
        this.repository = repository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional
    public ManualToolActivationResponse activate(UUID caseFileId,
                                                 ManualToolActivationRequest request,
                                                 OidcUser oidcUser,
                                                 Principal principal) {
        if (request == null || request.toolId() == null || request.toolId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "toolId requis");
        }
        String toolId = request.toolId().trim();
        if (toolId.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "toolId trop long (>128)");
        }

        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        CaseFile cf = resolveOwnedCaseFile(caseFileId, user);

        Optional<ManualToolActivation> existing =
                repository.findFirstByCaseFileIdAndToolIdAndDeactivatedAtIsNull(caseFileId, toolId);
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Outil déjà activé");
        }

        ManualToolActivation mta = new ManualToolActivation();
        mta.setCaseFileId(caseFileId);
        mta.setToolId(toolId);
        mta.setActivatedBy(user.getId());
        mta.setActivatedAt(Instant.now());
        mta.setWorkspaceId(cf.getWorkspace().getId());
        mta = repository.save(mta);

        return new ManualToolActivationResponse(mta.getId(), mta.getToolId(), mta.getActivatedAt());
    }

    @Transactional
    public void deactivate(UUID caseFileId, String toolId, OidcUser oidcUser, Principal principal) {
        if (toolId == null || toolId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "toolId requis");
        }
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        resolveOwnedCaseFile(caseFileId, user); // throws 404 if not owned

        // Idempotent : si aucune activation active, no-op silencieux.
        repository.findFirstByCaseFileIdAndToolIdAndDeactivatedAtIsNull(caseFileId, toolId.trim())
                .ifPresent(mta -> {
                    mta.setDeactivatedAt(Instant.now());
                    repository.save(mta);
                });
    }

    private CaseFile resolveOwnedCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        UUID userWorkspaceId = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId())
                .orElse(null);
        if (userWorkspaceId == null || !userWorkspaceId.equals(cf.getWorkspace().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        return cf;
    }
}
