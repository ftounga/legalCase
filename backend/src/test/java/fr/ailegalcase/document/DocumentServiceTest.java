package fr.ailegalcase.document;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.PAYMENT_REQUIRED;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private CaseFileRepository caseFileRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private fr.ailegalcase.storage.StorageService storageService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PlanLimitService planLimitService;
    @Mock private OidcUser oidcUser;

    private DocumentService service;

    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CASE_FILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @BeforeEach
    void setUp() {
        service = new DocumentService(documentRepository, caseFileRepository,
                currentUserResolver, workspaceMemberRepository,
                storageService, eventPublisher, planLimitService);
    }

    private void mockUserWorkspaceAndCaseFile() throws IOException {
        User user = new User();
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findById(CASE_FILE_ID)).thenReturn(Optional.of(caseFile));
        lenient().when(storageService.upload(anyString(), any(), anyString(), anyLong()))
                .thenReturn(null);
        lenient().when(documentRepository.save(any(Document.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().doNothing().when(eventPublisher).publishEvent(any());
    }

    // U-01 : quota non atteint (4/5 Starter) → upload OK
    @Test
    void upload_quotaNotReached_succeeds() throws Exception {
        mockUserWorkspaceAndCaseFile();
        when(documentRepository.countByCaseFileId(CASE_FILE_ID)).thenReturn(4L);
        when(planLimitService.getMaxDocumentsPerCaseFileForWorkspace(WORKSPACE_ID)).thenReturn(5);

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());

        service.upload(CASE_FILE_ID, file, oidcUser, "GOOGLE", null);

        verify(storageService).upload(anyString(), any(), anyString(), anyLong());
    }

    // U-02 : quota atteint (5/5 Starter) → 402
    @Test
    void upload_quotaReached_throws402() throws Exception {
        mockUserWorkspaceAndCaseFile();
        when(documentRepository.countByCaseFileId(CASE_FILE_ID)).thenReturn(5L);
        when(planLimitService.getMaxDocumentsPerCaseFileForWorkspace(WORKSPACE_ID)).thenReturn(5);

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> service.upload(CASE_FILE_ID, file, oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    var rse = (ResponseStatusException) ex;
                    assert rse.getStatusCode() == PAYMENT_REQUIRED;
                });

        verify(storageService, never()).upload(anyString(), any(), anyString(), anyLong());
    }

    // U-03 : pas de subscription → fail open, upload autorisé
    @Test
    void upload_noSubscription_failOpen() throws Exception {
        mockUserWorkspaceAndCaseFile();
        when(documentRepository.countByCaseFileId(CASE_FILE_ID)).thenReturn(100L);
        when(planLimitService.getMaxDocumentsPerCaseFileForWorkspace(WORKSPACE_ID)).thenReturn(Integer.MAX_VALUE);

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());

        service.upload(CASE_FILE_ID, file, oidcUser, "GOOGLE", null);

        verify(storageService).upload(anyString(), any(), anyString(), anyLong());
    }
}
