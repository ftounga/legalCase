package fr.ailegalcase.document;

import fr.ailegalcase.analysis.*;
import fr.ailegalcase.audit.AuditLog;
import fr.ailegalcase.audit.AuditLogRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
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
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class DocumentDeleteServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private CaseFileRepository caseFileRepository;
    @Mock private DocumentExtractionRepository documentExtractionRepository;
    @Mock private DocumentChunkRepository documentChunkRepository;
    @Mock private DocumentAnalysisRepository documentAnalysisRepository;
    @Mock private ChunkAnalysisRepository chunkAnalysisRepository;
    @Mock private AnalysisJobRepository analysisJobRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private OidcUser oidcUser;

    private DocumentDeleteService service;

    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CASE_FILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID DOCUMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @BeforeEach
    void setUp() {
        service = new DocumentDeleteService(
                documentRepository, caseFileRepository,
                documentExtractionRepository, documentChunkRepository,
                documentAnalysisRepository, chunkAnalysisRepository,
                analysisJobRepository, auditLogRepository,
                currentUserResolver, workspaceMemberRepository);
    }

    private void setupHappyPath() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);

        Workspace workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        WorkspaceMember member = mock(WorkspaceMember.class);
        when(member.getWorkspace()).thenReturn(workspace);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));

        CaseFile caseFile = mock(CaseFile.class);
        when(caseFile.getId()).thenReturn(CASE_FILE_ID);
        when(caseFile.getWorkspace()).thenReturn(workspace);
        when(caseFile.getTitle()).thenReturn("Test Case");
        when(caseFileRepository.findById(CASE_FILE_ID)).thenReturn(Optional.of(caseFile));

        Document document = mock(Document.class);
        when(document.getCaseFile()).thenReturn(caseFile);
        when(document.getOriginalFilename()).thenReturn("doc.pdf");
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

        when(analysisJobRepository.existsByCaseFileIdAndStatusIn(any(), any())).thenReturn(false);
        when(documentExtractionRepository.findByDocumentIdIn(any())).thenReturn(List.of());
    }

    @Test
    void delete_happyPath_deletesDocumentAndWritesAuditLog() {
        setupHappyPath();

        service.delete(CASE_FILE_ID, DOCUMENT_ID, oidcUser, "google", null);

        verify(documentRepository).deleteById(DOCUMENT_ID);
        verify(caseFileRepository).save(any(CaseFile.class));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo("DOCUMENT_DELETED");
        assertThat(log.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(log.getUserId()).isEqualTo(USER_ID);
        assertThat(log.getCaseFileId()).isEqualTo(CASE_FILE_ID);
        assertThat(log.getMetadata()).contains(DOCUMENT_ID.toString());
    }

    @Test
    void delete_analysisInProgress_throwsConflict() {
        User user = mock(User.class);
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);

        Workspace workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        WorkspaceMember member = mock(WorkspaceMember.class);
        when(member.getWorkspace()).thenReturn(workspace);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));

        CaseFile caseFile = mock(CaseFile.class);
        when(caseFile.getId()).thenReturn(CASE_FILE_ID);
        when(caseFile.getWorkspace()).thenReturn(workspace);
        when(caseFileRepository.findById(CASE_FILE_ID)).thenReturn(Optional.of(caseFile));

        Document document = mock(Document.class);
        when(document.getCaseFile()).thenReturn(caseFile);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

        when(analysisJobRepository.existsByCaseFileIdAndStatusIn(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.delete(CASE_FILE_ID, DOCUMENT_ID, oidcUser, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(CONFLICT));

        verify(documentRepository, never()).deleteById(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void delete_documentBelongsToOtherCaseFile_throwsNotFound() {
        User user = mock(User.class);
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);

        Workspace workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        WorkspaceMember member = mock(WorkspaceMember.class);
        when(member.getWorkspace()).thenReturn(workspace);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));

        CaseFile caseFile = mock(CaseFile.class);
        when(caseFile.getWorkspace()).thenReturn(workspace);
        when(caseFileRepository.findById(CASE_FILE_ID)).thenReturn(Optional.of(caseFile));

        UUID otherCaseFileId = UUID.randomUUID();
        CaseFile otherCaseFile = mock(CaseFile.class);
        when(otherCaseFile.getId()).thenReturn(otherCaseFileId);
        Document document = mock(Document.class);
        when(document.getCaseFile()).thenReturn(otherCaseFile);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.delete(CASE_FILE_ID, DOCUMENT_ID, oidcUser, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(NOT_FOUND));

        verify(documentRepository, never()).deleteById(any());
    }
}
