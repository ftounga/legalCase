package fr.ailegalcase.casefile;

import fr.ailegalcase.analysis.AnalysisJobRepository;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.audit.AuditLog;
import fr.ailegalcase.audit.AuditLogRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.ExtractionStatus;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CaseFileStatusService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final List<AnalysisStatus> ACTIVE_STATUSES =
            List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING);
    private static final List<ExtractionStatus> ACTIVE_EXTRACTION_STATUSES =
            List.of(ExtractionStatus.PENDING, ExtractionStatus.PROCESSING);

    private final CaseFileRepository caseFileRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final DocumentExtractionRepository documentExtractionRepository;
    private final AuditLogRepository auditLogRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final PlanLimitService planLimitService;
    private final CurrentUserResolver currentUserResolver;

    public CaseFileStatusService(CaseFileRepository caseFileRepository,
                                 AnalysisJobRepository analysisJobRepository,
                                 DocumentExtractionRepository documentExtractionRepository,
                                 AuditLogRepository auditLogRepository,
                                 WorkspaceMemberRepository workspaceMemberRepository,
                                 PlanLimitService planLimitService,
                                 CurrentUserResolver currentUserResolver) {
        this.caseFileRepository = caseFileRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.documentExtractionRepository = documentExtractionRepository;
        this.auditLogRepository = auditLogRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.planLimitService = planLimitService;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional
    public CaseFileResponse close(UUID caseFileId, OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        WorkspaceMember member = resolveMembership(user);
        CaseFile caseFile = resolveCaseFile(caseFileId, member.getWorkspace().getId());

        if (isPipelineActive(caseFileId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une analyse est en cours sur ce dossier. Attendez la fin avant de le clôturer.");
        }

        if (STATUS_OPEN.equals(caseFile.getStatus())) {
            caseFile.setStatus(STATUS_CLOSED);
            caseFileRepository.save(caseFile);
            saveAuditLog("CASE_FILE_CLOSED", caseFile, user, member.getWorkspace().getId());
        }

        return toResponse(caseFile);
    }

    @Transactional
    public CaseFileResponse reopen(UUID caseFileId, OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        WorkspaceMember member = resolveMembership(user);
        requireOwnerOrAdmin(member);

        CaseFile caseFile = resolveCaseFile(caseFileId, member.getWorkspace().getId());

        if (STATUS_CLOSED.equals(caseFile.getStatus())) {
            UUID workspaceId = member.getWorkspace().getId();
            long openCount = caseFileRepository.countByWorkspace_IdAndStatusAndDeletedAtIsNull(workspaceId, STATUS_OPEN);
            int maxOpen = planLimitService.getMaxOpenCaseFilesForWorkspace(workspaceId);
            if (openCount >= maxOpen) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                        "Limite de dossiers actifs atteinte. Passez à un plan supérieur.");
            }

            caseFile.setStatus(STATUS_OPEN);
            caseFileRepository.save(caseFile);
            saveAuditLog("CASE_FILE_REOPENED", caseFile, user, workspaceId);
        }

        return toResponse(caseFile);
    }

    @Transactional
    public void delete(UUID caseFileId, OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        WorkspaceMember member = resolveMembership(user);
        requireOwner(member);

        CaseFile caseFile = resolveCaseFile(caseFileId, member.getWorkspace().getId());

        if (isPipelineActive(caseFileId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une analyse est en cours sur ce dossier. Attendez la fin avant de le supprimer.");
        }

        caseFile.setDeletedAt(Instant.now());
        caseFileRepository.save(caseFile);
        saveAuditLog("CASE_FILE_DELETED", caseFile, user, member.getWorkspace().getId());
    }

    private boolean isPipelineActive(UUID caseFileId) {
        return analysisJobRepository.existsByCaseFileIdAndStatusIn(caseFileId, ACTIVE_STATUSES)
                || documentExtractionRepository.existsByDocumentCaseFileIdAndExtractionStatusIn(
                        caseFileId, ACTIVE_EXTRACTION_STATUSES);
    }

    private WorkspaceMember resolveMembership(User user) {
        return workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
    }

    private CaseFile resolveCaseFile(UUID caseFileId, UUID workspaceId) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspaceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        return caseFile;
    }

    private void requireOwnerOrAdmin(WorkspaceMember member) {
        String role = member.getMemberRole();
        if (!"OWNER".equals(role) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private void requireOwner(WorkspaceMember member) {
        if (!"OWNER".equals(member.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
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

    private CaseFileResponse toResponse(CaseFile cf) {
        return new CaseFileResponse(cf.getId(), cf.getTitle(), cf.getLegalDomain(),
                cf.getDescription(), cf.getStatus(), cf.getCreatedAt(), cf.getLastDocumentDeletedAt());
    }
}
