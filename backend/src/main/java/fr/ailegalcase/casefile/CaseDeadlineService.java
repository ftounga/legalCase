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
import java.util.List;
import java.util.UUID;

@Service
public class CaseDeadlineService {

    private final CaseDeadlineRepository deadlineRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public CaseDeadlineService(CaseDeadlineRepository deadlineRepository,
                               CaseFileRepository caseFileRepository,
                               WorkspaceMemberRepository workspaceMemberRepository,
                               CurrentUserResolver currentUserResolver) {
        this.deadlineRepository = deadlineRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public List<CaseDeadlineResponse> list(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        return deadlineRepository.findByCaseFileIdOrderByDueDateAsc(caseFile.getId())
                .stream().map(CaseDeadlineResponse::from).toList();
    }

    @Transactional
    public CaseDeadlineResponse create(UUID caseFileId, CaseDeadlineRequest request,
                                       OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);

        CaseDeadline deadline = new CaseDeadline();
        deadline.setCaseFile(caseFile);
        deadline.setLabel(request.label().trim());
        deadline.setDueDate(request.dueDate());
        return CaseDeadlineResponse.from(deadlineRepository.save(deadline));
    }

    @Transactional
    public CaseDeadlineResponse update(UUID caseFileId, UUID deadlineId, CaseDeadlineRequest request,
                                       OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFileForUser(caseFileId, user);
        CaseDeadline deadline = resolveDeadlineForWorkspace(deadlineId, caseFileId);
        deadline.setLabel(request.label().trim());
        deadline.setDueDate(request.dueDate());
        return CaseDeadlineResponse.from(deadlineRepository.save(deadline));
    }

    @Transactional
    public void delete(UUID caseFileId, UUID deadlineId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFileForUser(caseFileId, user);
        CaseDeadline deadline = resolveDeadlineForWorkspace(deadlineId, caseFileId);
        deadlineRepository.delete(deadline);
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private CaseFile resolveCaseFileForUser(UUID caseFileId, User user) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(caseFile.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        return caseFile;
    }

    private CaseDeadline resolveDeadlineForWorkspace(UUID deadlineId, UUID caseFileId) {
        return deadlineRepository.findById(deadlineId)
                .filter(d -> d.getCaseFile().getId().equals(caseFileId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deadline not found"));
    }
}
