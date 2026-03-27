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
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;

@ExtendWith(MockitoExtension.class)
class CaseFileStatusServiceTest {

    @Mock private CaseFileRepository caseFileRepository;
    @Mock private AnalysisJobRepository analysisJobRepository;
    @Mock private DocumentExtractionRepository documentExtractionRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private PlanLimitService planLimitService;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private OidcUser oidcUser;

    private CaseFileStatusService service;

    private User user;
    private Workspace workspace;
    private WorkspaceMember ownerMember;
    private WorkspaceMember adminMember;
    private WorkspaceMember lawyerMember;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        service = new CaseFileStatusService(caseFileRepository, analysisJobRepository,
                documentExtractionRepository, auditLogRepository, workspaceMemberRepository,
                planLimitService, currentUserResolver);

        user = new User();
        user.setId(UUID.randomUUID());

        workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        ownerMember = new WorkspaceMember();
        ownerMember.setUser(user);
        ownerMember.setWorkspace(workspace);
        ownerMember.setMemberRole("OWNER");

        adminMember = new WorkspaceMember();
        adminMember.setUser(user);
        adminMember.setWorkspace(workspace);
        adminMember.setMemberRole("ADMIN");

        lawyerMember = new WorkspaceMember();
        lawyerMember.setUser(user);
        lawyerMember.setWorkspace(workspace);
        lawyerMember.setMemberRole("LAWYER");

        caseFile = new CaseFile();
        caseFile.setId(UUID.randomUUID());
        caseFile.setTitle("Dossier test");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile.setWorkspace(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
    }

    // --- close ---

    @Test
    void close_nominal_setsStatusClosedAndLogsAudit() {
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(ownerMember));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFile.getId())).thenReturn(Optional.of(caseFile));
        when(analysisJobRepository.existsByCaseFileIdAndStatusIn(caseFile.getId(),
                List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING))).thenReturn(false);
        when(documentExtractionRepository.existsByDocumentCaseFileIdAndExtractionStatusIn(caseFile.getId(),
                List.of(ExtractionStatus.PENDING, ExtractionStatus.PROCESSING))).thenReturn(false);

        service.close(caseFile.getId(), oidcUser, "GOOGLE", null);

        assertThat(caseFile.getStatus()).isEqualTo("CLOSED");
        verify(caseFileRepository).save(caseFile);
        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo("CASE_FILE_CLOSED");
    }

    @Test
    void close_throws409IfAnalysisRunning() {
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(ownerMember));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFile.getId())).thenReturn(Optional.of(caseFile));
        when(analysisJobRepository.existsByCaseFileIdAndStatusIn(caseFile.getId(),
                List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING))).thenReturn(true);

        assertThatThrownBy(() -> service.close(caseFile.getId(), oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(CONFLICT);
    }

    @Test
    void close_throws409IfExtractionRunning() {
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(ownerMember));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFile.getId())).thenReturn(Optional.of(caseFile));
        when(analysisJobRepository.existsByCaseFileIdAndStatusIn(caseFile.getId(),
                List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING))).thenReturn(false);
        when(documentExtractionRepository.existsByDocumentCaseFileIdAndExtractionStatusIn(caseFile.getId(),
                List.of(ExtractionStatus.PENDING, ExtractionStatus.PROCESSING))).thenReturn(true);

        assertThatThrownBy(() -> service.close(caseFile.getId(), oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(CONFLICT);
    }

    @Test
    void close_idempotent_doesNothingIfAlreadyClosed() {
        caseFile.setStatus("CLOSED");
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(lawyerMember));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFile.getId())).thenReturn(Optional.of(caseFile));
        when(analysisJobRepository.existsByCaseFileIdAndStatusIn(caseFile.getId(),
                List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING))).thenReturn(false);
        when(documentExtractionRepository.existsByDocumentCaseFileIdAndExtractionStatusIn(caseFile.getId(),
                List.of(ExtractionStatus.PENDING, ExtractionStatus.PROCESSING))).thenReturn(false);

        service.close(caseFile.getId(), oidcUser, "GOOGLE", null);

        verify(caseFileRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void close_throws404IfAnotherWorkspace() {
        Workspace other = new Workspace();
        other.setId(UUID.randomUUID());
        caseFile.setWorkspace(other);

        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(ownerMember));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFile.getId())).thenReturn(Optional.of(caseFile));

        assertThatThrownBy(() -> service.close(caseFile.getId(), oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(NOT_FOUND);
    }

    // --- reopen ---

    @Test
    void reopen_nominal_setsStatusOpenAndLogsAudit() {
        caseFile.setStatus("CLOSED");
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(ownerMember));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFile.getId())).thenReturn(Optional.of(caseFile));
        when(caseFileRepository.countByWorkspace_IdAndStatusAndDeletedAtIsNull(workspace.getId(), "OPEN")).thenReturn(0L);
        when(planLimitService.getMaxOpenCaseFilesForWorkspace(workspace.getId())).thenReturn(3);

        service.reopen(caseFile.getId(), oidcUser, "GOOGLE", null);

        assertThat(caseFile.getStatus()).isEqualTo("OPEN");
        verify(caseFileRepository).save(caseFile);
        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo("CASE_FILE_REOPENED");
    }

    @Test
    void reopen_throws402IfQuotaReached() {
        caseFile.setStatus("CLOSED");
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(ownerMember));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFile.getId())).thenReturn(Optional.of(caseFile));
        when(caseFileRepository.countByWorkspace_IdAndStatusAndDeletedAtIsNull(workspace.getId(), "OPEN")).thenReturn(3L);
        when(planLimitService.getMaxOpenCaseFilesForWorkspace(workspace.getId())).thenReturn(3);

        assertThatThrownBy(() -> service.reopen(caseFile.getId(), oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(PAYMENT_REQUIRED);
    }

    @Test
    void reopen_throws403IfLawyer() {
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(lawyerMember));

        assertThatThrownBy(() -> service.reopen(caseFile.getId(), oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(FORBIDDEN);
    }

    // --- delete ---

    @Test
    void delete_nominal_setsDeletedAtAndLogsAudit() {
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(ownerMember));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFile.getId())).thenReturn(Optional.of(caseFile));
        when(analysisJobRepository.existsByCaseFileIdAndStatusIn(caseFile.getId(),
                List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING))).thenReturn(false);
        when(documentExtractionRepository.existsByDocumentCaseFileIdAndExtractionStatusIn(caseFile.getId(),
                List.of(ExtractionStatus.PENDING, ExtractionStatus.PROCESSING))).thenReturn(false);

        service.delete(caseFile.getId(), oidcUser, "GOOGLE", null);

        assertThat(caseFile.getDeletedAt()).isNotNull();
        verify(caseFileRepository).save(caseFile);
        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo("CASE_FILE_DELETED");
    }

    @Test
    void delete_throws403IfAdmin() {
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(adminMember));

        assertThatThrownBy(() -> service.delete(caseFile.getId(), oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(FORBIDDEN);
    }

    @Test
    void delete_throws409IfAnalysisRunning() {
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(ownerMember));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFile.getId())).thenReturn(Optional.of(caseFile));
        when(analysisJobRepository.existsByCaseFileIdAndStatusIn(caseFile.getId(),
                List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING))).thenReturn(true);

        assertThatThrownBy(() -> service.delete(caseFile.getId(), oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(CONFLICT);
    }

    @Test
    void delete_throws409IfExtractionRunning() {
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(ownerMember));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFile.getId())).thenReturn(Optional.of(caseFile));
        when(analysisJobRepository.existsByCaseFileIdAndStatusIn(caseFile.getId(),
                List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING))).thenReturn(false);
        when(documentExtractionRepository.existsByDocumentCaseFileIdAndExtractionStatusIn(caseFile.getId(),
                List.of(ExtractionStatus.PENDING, ExtractionStatus.PROCESSING))).thenReturn(true);

        assertThatThrownBy(() -> service.delete(caseFile.getId(), oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(CONFLICT);
    }
}
