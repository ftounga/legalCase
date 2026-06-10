package fr.ailegalcase.document;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.PaymentRequiredCode;
import fr.ailegalcase.shared.PaymentRequiredException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.PAYMENT_REQUIRED;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentExtractionRepository extractionRepository;
    @Mock private CaseFileRepository caseFileRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private fr.ailegalcase.storage.StorageService storageService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PlanLimitService planLimitService;
    @Mock private DocumentPieceRepository documentPieceRepository;
    @Mock private fr.ailegalcase.video.VideoQuotaService videoQuotaService;
    @Mock private OidcUser oidcUser;

    private DocumentService service;

    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CASE_FILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @BeforeEach
    void setUp() {
        service = new DocumentService(documentRepository, extractionRepository, caseFileRepository,
                currentUserResolver, workspaceMemberRepository,
                storageService, eventPublisher, planLimitService, documentPieceRepository,
                videoQuotaService);
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
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CASE_FILE_ID)).thenReturn(Optional.of(caseFile));
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
                .isInstanceOf(PaymentRequiredException.class)
                .satisfies(ex -> assertThat(((PaymentRequiredException) ex).getCode())
                        .isEqualTo(PaymentRequiredCode.DOCUMENT_LIMIT_EXCEEDED));

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

    // SF-144-01 U-02 : isOcrExtracted reconnaît "textract" et "textract-rasterized"
    @Test
    void isOcrExtracted_textractMetadata_returnsTrue() {
        assertThat(DocumentService.isOcrExtracted("{\"extractor\":\"textract\",\"pageCount\":3}")).isTrue();
        assertThat(DocumentService.isOcrExtracted("{\"extractor\":\"textract-rasterized\",\"pageCount\":5}")).isTrue();
    }

    @Test
    void isOcrExtracted_internalOrNull_returnsFalse() {
        assertThat(DocumentService.isOcrExtracted("{\"extractor\":\"internal\",\"charCount\":1000}")).isFalse();
        assertThat(DocumentService.isOcrExtracted("{\"extractor\":\"internal+textract\",\"reason\":\"OCR_FAILED\"}")).isFalse();
        assertThat(DocumentService.isOcrExtracted(null)).isFalse();
        assertThat(DocumentService.isOcrExtracted("")).isFalse();
    }

    // ── SF-261-01 : marquage « écritures adverses » ──────────────────────────

    private static final UUID DOCUMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    /** Prépare user/workspace/caseFile + un document appartenant au dossier. */
    private Document mockUserWorkspaceCaseFileAndDocument() {
        User user = new User();
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        CaseFile caseFile = new CaseFile();
        caseFile.setId(CASE_FILE_ID);
        caseFile.setWorkspace(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);

        Document document = new Document();
        document.setId(DOCUMENT_ID);
        document.setCaseFile(caseFile);
        document.setOriginalFilename("conclusions-adverses.pdf");
        document.setContentType("application/pdf");
        document.setFileSize(1024L);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CASE_FILE_ID)).thenReturn(Optional.of(caseFile));
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
        lenient().when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(extractionRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(Optional.empty());
        lenient().when(documentPieceRepository.findByDocument_IdOrderByOrderIndexAsc(DOCUMENT_ID))
                .thenReturn(java.util.List.of());
        return document;
    }

    // U-AP-01 : marquage true persiste et est exposé dans la réponse
    @Test
    void markAdversePleadings_true_persistsAndReturnsFlag() {
        Document document = mockUserWorkspaceCaseFileAndDocument();

        DocumentResponse response = service.markAdversePleadings(
                CASE_FILE_ID, DOCUMENT_ID, true, oidcUser, "GOOGLE", null);

        assertThat(document.isAdversePleadings()).isTrue();
        assertThat(response.adversePleadings()).isTrue();
        verify(documentRepository).save(document);
    }

    // U-AP-02 : démarquage (false) persiste à false
    @Test
    void markAdversePleadings_false_persistsAndReturnsFlag() {
        Document document = mockUserWorkspaceCaseFileAndDocument();
        document.setAdversePleadings(true);

        DocumentResponse response = service.markAdversePleadings(
                CASE_FILE_ID, DOCUMENT_ID, false, oidcUser, "GOOGLE", null);

        assertThat(document.isAdversePleadings()).isFalse();
        assertThat(response.adversePleadings()).isFalse();
    }

    // U-AP-03 : document d'un dossier d'un autre workspace → 404 (isolation)
    @Test
    void markAdversePleadings_caseFileOtherWorkspace_returns404() {
        User user = new User();
        Workspace myWorkspace = new Workspace();
        myWorkspace.setId(WORKSPACE_ID);
        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setId(UUID.randomUUID());
        CaseFile caseFile = new CaseFile();
        caseFile.setId(CASE_FILE_ID);
        caseFile.setWorkspace(otherWorkspace); // dossier d'un AUTRE workspace

        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(myWorkspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CASE_FILE_ID)).thenReturn(Optional.of(caseFile));

        assertThatThrownBy(() -> service.markAdversePleadings(
                CASE_FILE_ID, DOCUMENT_ID, true, oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Case file not found");

        verify(documentRepository, never()).save(any());
    }

    // U-AP-04 : document appartenant à un AUTRE dossier (même workspace) → 404
    @Test
    void markAdversePleadings_documentInAnotherCaseFile_returns404() {
        Document document = mockUserWorkspaceCaseFileAndDocument();
        CaseFile autreDossier = new CaseFile();
        autreDossier.setId(UUID.randomUUID());
        autreDossier.setWorkspace(document.getCaseFile().getWorkspace());
        document.setCaseFile(autreDossier); // le doc pointe sur un autre dossier

        assertThatThrownBy(() -> service.markAdversePleadings(
                CASE_FILE_ID, DOCUMENT_ID, true, oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Document not found");
    }

    // U-AP-05 : document inexistant → 404
    @Test
    void markAdversePleadings_unknownDocument_returns404() {
        User user = new User();
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        CaseFile caseFile = new CaseFile();
        caseFile.setId(CASE_FILE_ID);
        caseFile.setWorkspace(workspace);
        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CASE_FILE_ID)).thenReturn(Optional.of(caseFile));
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAdversePleadings(
                CASE_FILE_ID, DOCUMENT_ID, true, oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Document not found");
    }
}
