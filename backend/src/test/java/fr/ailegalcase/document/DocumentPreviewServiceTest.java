package fr.ailegalcase.document;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentPreviewServiceTest {

    private DocumentRepository documentRepository;
    private DocumentExtractionRepository extractionRepository;
    private CaseFileRepository caseFileRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private CurrentUserResolver currentUserResolver;
    private DocumentPreviewService service;

    private User user;
    private Workspace workspace;
    private CaseFile caseFile;

    @BeforeEach
    void setup() {
        documentRepository = mock(DocumentRepository.class);
        extractionRepository = mock(DocumentExtractionRepository.class);
        caseFileRepository = mock(CaseFileRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        currentUserResolver = mock(CurrentUserResolver.class);

        service = new DocumentPreviewService(documentRepository, extractionRepository,
                caseFileRepository, workspaceMemberRepository, currentUserResolver);

        user = new User();
        user.setId(UUID.randomUUID());
        workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        caseFile = new CaseFile();
        caseFile.setId(UUID.randomUUID());
        caseFile.setWorkspace(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findById(caseFile.getId())).thenReturn(Optional.of(caseFile));
    }

    @Test
    void preview_classicExtractionDone_returnsTextWithMethodClassic() {
        Document doc = buildDoc(caseFile, "contrat.pdf", "application/pdf", 500_000L);
        DocumentExtraction ex = new DocumentExtraction();
        ex.setExtractionStatus(ExtractionStatus.DONE);
        ex.setExtractedText("Contrat de travail CDI signé le 15 mars 2024.");
        ex.setExtractionMetadata("{\"extractor\":\"PDFBOX\",\"pageCount\":3,\"charCount\":45}");
        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(extractionRepository.findByDocumentId(doc.getId())).thenReturn(Optional.of(ex));

        DocumentPreviewResponse result = service.preview(caseFile.getId(), doc.getId(), null, "google", null);

        assertThat(result.extractionStatus()).isEqualTo(ExtractionStatus.DONE);
        assertThat(result.extractionMethod()).isEqualTo(DocumentPreviewResponse.ExtractionMethod.CLASSIC);
        assertThat(result.extractedText()).contains("CDI");
        assertThat(result.charCount()).isEqualTo(45);
        assertThat(result.textTruncated()).isFalse();
        assertThat(result.pageCount()).isEqualTo(3);
        assertThat(result.ocrPagesUsed()).isZero();
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void preview_ocrExtractionDone_returnsMethodOcrWithPagesUsed() {
        Document doc = buildDoc(caseFile, "scan.pdf", "application/pdf", 2_000_000L);
        DocumentExtraction ex = new DocumentExtraction();
        ex.setExtractionStatus(ExtractionStatus.DONE);
        ex.setExtractedText("Texte OCR.");
        ex.setExtractionMetadata("{\"extractor\":\"TEXTRACT\",\"formsMode\":false,\"pageCount\":5,\"quotaPages\":5}");
        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(extractionRepository.findByDocumentId(doc.getId())).thenReturn(Optional.of(ex));

        DocumentPreviewResponse result = service.preview(caseFile.getId(), doc.getId(), null, "google", null);

        assertThat(result.extractionMethod()).isEqualTo(DocumentPreviewResponse.ExtractionMethod.OCR);
        assertThat(result.ocrPagesUsed()).isEqualTo(5);
    }

    @Test
    void preview_failedExtraction_returnsFailureReason() {
        Document doc = buildDoc(caseFile, "corrupt.pdf", "application/pdf", 100L);
        DocumentExtraction ex = new DocumentExtraction();
        ex.setExtractionStatus(ExtractionStatus.FAILED);
        ex.setFailureReason(ExtractionFailureReason.EMPTY_TEXT);
        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(extractionRepository.findByDocumentId(doc.getId())).thenReturn(Optional.of(ex));

        DocumentPreviewResponse result = service.preview(caseFile.getId(), doc.getId(), null, "google", null);

        assertThat(result.extractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(ExtractionFailureReason.EMPTY_TEXT);
        assertThat(result.extractedText()).isNull();
    }

    @Test
    void preview_longText_isTruncatedAt200K() {
        Document doc = buildDoc(caseFile, "long.pdf", "application/pdf", 10_000_000L);
        String bigText = "A".repeat(250_000);
        DocumentExtraction ex = new DocumentExtraction();
        ex.setExtractionStatus(ExtractionStatus.DONE);
        ex.setExtractedText(bigText);
        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(extractionRepository.findByDocumentId(doc.getId())).thenReturn(Optional.of(ex));

        DocumentPreviewResponse result = service.preview(caseFile.getId(), doc.getId(), null, "google", null);

        assertThat(result.charCount()).isEqualTo(250_000);
        assertThat(result.extractedText()).hasSize(DocumentPreviewResponse.TEXT_TRUNCATE_LIMIT);
        assertThat(result.textTruncated()).isTrue();
    }

    @Test
    void preview_documentFromOtherWorkspace_throws404() {
        Workspace otherWs = new Workspace();
        otherWs.setId(UUID.randomUUID());
        CaseFile otherCf = new CaseFile();
        otherCf.setId(UUID.randomUUID());
        otherCf.setWorkspace(otherWs);
        when(caseFileRepository.findById(otherCf.getId())).thenReturn(Optional.of(otherCf));

        assertThatThrownBy(() -> service.preview(otherCf.getId(), UUID.randomUUID(), null, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void preview_documentBelongsToOtherCaseFile_throws404() {
        Document doc = buildDoc(caseFile, "doc.pdf", "application/pdf", 1000L);
        // Réassocie le doc à un autre caseFile
        CaseFile otherCf = new CaseFile();
        otherCf.setId(UUID.randomUUID());
        otherCf.setWorkspace(workspace);
        doc.setCaseFile(otherCf);
        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.preview(caseFile.getId(), doc.getId(), null, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    private Document buildDoc(CaseFile cf, String name, String mime, long size) {
        Document d = new Document();
        d.setId(UUID.randomUUID());
        d.setCaseFile(cf);
        d.setOriginalFilename(name);
        d.setContentType(mime);
        d.setFileSize(size);
        d.setStorageKey("k/" + d.getId());
        // manual init since Document uses @PrePersist
        try {
            var field = Document.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(d, Instant.now());
        } catch (Exception ignored) {}
        return d;
    }
}
