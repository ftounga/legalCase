package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.UUID;

@Service
public class CaseFileService {

    private static final String SUPPORTED_LEGAL_DOMAIN = "DROIT_DU_TRAVAIL";

    private final CaseFileRepository caseFileRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final PlanLimitService planLimitService;

    public CaseFileService(CaseFileRepository caseFileRepository,
                           CurrentUserResolver currentUserResolver,
                           WorkspaceMemberRepository workspaceMemberRepository,
                           PlanLimitService planLimitService) {
        this.caseFileRepository = caseFileRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.planLimitService = planLimitService;
    }

    @Transactional
    public CaseFileResponse create(CaseFileRequest request, OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        Workspace workspace = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        if (!SUPPORTED_LEGAL_DOMAIN.equals(workspace.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seul le droit du travail est supporté en V1");
        }

        long openCount = caseFileRepository.countByWorkspace_IdAndStatus(workspace.getId(), "OPEN");
        int maxOpen = planLimitService.getMaxOpenCaseFilesForWorkspace(workspace.getId());
        if (openCount >= maxOpen) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Limite de dossiers actifs atteinte pour votre plan.");
        }

        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFile.setTitle(request.title().trim());
        caseFile.setLegalDomain(workspace.getLegalDomain());
        caseFile.setDescription(request.description());
        caseFile.setStatus("OPEN");
        caseFileRepository.save(caseFile);

        return new CaseFileResponse(caseFile.getId(), caseFile.getTitle(), caseFile.getLegalDomain(),
                caseFile.getDescription(), caseFile.getStatus(), caseFile.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Page<CaseFileResponse> list(OidcUser oidcUser, String provider, Pageable pageable, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        Workspace workspace = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        return caseFileRepository.findByWorkspace(workspace, pageable)
                .map(cf -> new CaseFileResponse(cf.getId(), cf.getTitle(), cf.getLegalDomain(),
                        cf.getDescription(), cf.getStatus(), cf.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public CaseFileResponse getById(UUID id, OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        Workspace workspace = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));

        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        return new CaseFileResponse(caseFile.getId(), caseFile.getTitle(), caseFile.getLegalDomain(),
                caseFile.getDescription(), caseFile.getStatus(), caseFile.getCreatedAt());
    }
}
