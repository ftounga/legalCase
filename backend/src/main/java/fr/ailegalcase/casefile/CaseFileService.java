package fr.ailegalcase.casefile;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.audit.AuditLog;
import fr.ailegalcase.audit.AuditLogRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

@Service
public class CaseFileService {

    private final CaseFileRepository caseFileRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final PlanLimitService planLimitService;
    private final AuditLogRepository auditLogRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;

    public CaseFileService(CaseFileRepository caseFileRepository,
                           CurrentUserResolver currentUserResolver,
                           WorkspaceMemberRepository workspaceMemberRepository,
                           PlanLimitService planLimitService,
                           AuditLogRepository auditLogRepository,
                           CaseAnalysisRepository caseAnalysisRepository) {
        this.caseFileRepository = caseFileRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.planLimitService = planLimitService;
        this.auditLogRepository = auditLogRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
    }

    @Transactional
    public CaseFileResponse create(CaseFileRequest request, OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        Workspace workspace = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        long openCount = caseFileRepository.countByWorkspace_IdAndStatusAndDeletedAtIsNull(workspace.getId(), "OPEN");
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
                caseFile.getDescription(), caseFile.getStatus(), caseFile.getCreatedAt(),
                caseFile.getLastDocumentDeletedAt(), null, null);
    }

    @Transactional(readOnly = true)
    public Page<CaseFileResponse> list(OidcUser oidcUser, String provider, Pageable pageable, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        Workspace workspace = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        return caseFileRepository.findByWorkspaceAndDeletedAtIsNull(workspace, pageable)
                .map(cf -> {
                    var latestAnalysis = caseAnalysisRepository
                            .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(cf.getId(), AnalysisStatus.DONE)
                            .orElse(null);
                    String riskLevel = latestAnalysis != null ? latestAnalysis.getRiskLevel() : null;
                    Integer riskScore = latestAnalysis != null ? latestAnalysis.getRiskScore() : null;
                    return new CaseFileResponse(cf.getId(), cf.getTitle(), cf.getLegalDomain(),
                            cf.getDescription(), cf.getStatus(), cf.getCreatedAt(),
                            cf.getLastDocumentDeletedAt(), riskLevel, riskScore);
                });
    }

    @Transactional(readOnly = true)
    public CaseFileResponse getById(UUID id, OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        Workspace workspace = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));

        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        return new CaseFileResponse(caseFile.getId(), caseFile.getTitle(), caseFile.getLegalDomain(),
                caseFile.getDescription(), caseFile.getStatus(), caseFile.getCreatedAt(),
                caseFile.getLastDocumentDeletedAt(), null, null);
    }

    @Transactional
    public CaseFileResponse update(UUID id, CaseFileUpdateRequest request,
                                   OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        WorkspaceMember member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        UUID workspaceId = member.getWorkspace().getId();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));

        if (!caseFile.getWorkspace().getId().equals(workspaceId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        caseFile.setTitle(request.title().trim());
        caseFile.setDescription(request.description());
        caseFile.setUpdatedAt(Instant.now());
        CaseFile updated = caseFileRepository.save(caseFile);

        saveAuditLog("CASE_FILE_UPDATED", updated, user, workspaceId);

        return new CaseFileResponse(updated.getId(), updated.getTitle(), updated.getLegalDomain(),
                updated.getDescription(), updated.getStatus(), updated.getCreatedAt(),
                updated.getLastDocumentDeletedAt(), null, null);
    }

    private void saveAuditLog(String action, CaseFile caseFile, User user, UUID workspaceId) {
        AuditLog log = new AuditLog();
        log.setWorkspaceId(workspaceId);
        log.setUserId(user.getId());
        log.setCaseFileId(caseFile.getId());
        log.setAction(action);
        log.setMetadata("{\"caseFileTitle\":\"%s\"}".formatted(escape(caseFile.getTitle())));
        auditLogRepository.save(log);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
