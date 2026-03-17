package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class CaseFileService {

    private static final String SUPPORTED_LEGAL_DOMAIN = "EMPLOYMENT_LAW";

    private final CaseFileRepository caseFileRepository;
    private final AuthAccountRepository authAccountRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public CaseFileService(CaseFileRepository caseFileRepository,
                           AuthAccountRepository authAccountRepository,
                           WorkspaceMemberRepository workspaceMemberRepository) {
        this.caseFileRepository = caseFileRepository;
        this.authAccountRepository = authAccountRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public CaseFileResponse create(CaseFileRequest request, OidcUser oidcUser, String provider) {
        if (!SUPPORTED_LEGAL_DOMAIN.equals(request.legalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only EMPLOYMENT_LAW is supported in V1");
        }

        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getUser();

        Workspace workspace = workspaceMemberRepository
                .findFirstByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFile.setTitle(request.title().trim());
        caseFile.setLegalDomain(request.legalDomain());
        caseFile.setDescription(request.description());
        caseFile.setStatus("OPEN");
        caseFileRepository.save(caseFile);

        return new CaseFileResponse(caseFile.getId(), caseFile.getTitle(), caseFile.getLegalDomain(),
                caseFile.getDescription(), caseFile.getStatus(), caseFile.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Page<CaseFileResponse> list(OidcUser oidcUser, String provider, Pageable pageable) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getUser();

        Workspace workspace = workspaceMemberRepository
                .findFirstByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        return caseFileRepository.findByWorkspace(workspace, pageable)
                .map(cf -> new CaseFileResponse(cf.getId(), cf.getTitle(), cf.getLegalDomain(),
                        cf.getDescription(), cf.getStatus(), cf.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public CaseFileResponse getById(UUID id, OidcUser oidcUser, String provider) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getUser();

        Workspace workspace = workspaceMemberRepository
                .findFirstByUser(user)
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
