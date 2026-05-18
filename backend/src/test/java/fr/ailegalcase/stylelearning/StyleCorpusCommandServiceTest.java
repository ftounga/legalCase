package fr.ailegalcase.stylelearning;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.storage.StorageService;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * F-98 / SF-98-46 — tests unitaires des commandes du corpus de style :
 * upload nominal, gardes 400 (type / taille / fichier absent), isolation 404,
 * patch active, delete.
 */
class StyleCorpusCommandServiceTest {

    private final StyleCorpusRepository styleCorpusRepository = mock(StyleCorpusRepository.class);
    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final StorageService storageService = mock(StorageService.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    private final StyleCorpusCommandService service = new StyleCorpusCommandService(
            styleCorpusRepository, currentUserResolver, workspaceMemberRepository,
            storageService, rabbitTemplate);

    private User user;
    private Workspace workspace;
    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        workspaceId = UUID.randomUUID();
        workspace = new Workspace();
        ReflectionTestUtils.setField(workspace, "id", workspaceId);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setPrimary(true);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(styleCorpusRepository.save(any())).thenAnswer(inv -> {
            StyleCorpusDocument d = inv.getArgument(0);
            if (d.getId() == null) {
                ReflectionTestUtils.setField(d, "id", UUID.randomUUID());
            }
            return d;
        });
    }

    private MultipartFile pdfFile() {
        return new MockMultipartFile("file", "conclusion.pdf", "application/pdf",
                "%PDF-1.4 content".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void upload_nominal_createsPendingRowAndPublishesMessage() {
        StyleCorpusUploadResponse response = service.upload(
                workspaceId, pdfFile(), null, "GOOGLE", () -> "p");

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(StyleCorpusDocumentStatus.PENDING);

        ArgumentCaptorHolder.<StyleCorpusDocument>verifySaved(styleCorpusRepository, doc -> {
            assertThat(doc.getStatus()).isEqualTo(StyleCorpusDocumentStatus.PENDING);
            assertThat(doc.isActive()).isTrue();
            assertThat(doc.getOriginalFilename()).isEqualTo("conclusion.pdf");
            assertThat(doc.getWorkspace()).isSameAs(workspace);
            assertThat(doc.getUploadedBy()).isSameAs(user);
        });
        verify(storageService).upload(any(), any(), eq("application/pdf"), anyLong());
        verify(rabbitTemplate).convertAndSend(
                eq(StyleCorpusRabbitMQConfig.STYLE_CORPUS_EXCHANGE),
                eq(StyleCorpusRabbitMQConfig.STYLE_CORPUS_ROUTING_KEY),
                any(StyleCorpusMessage.class));
    }

    @Test
    void upload_unsupportedType_returns400() {
        MultipartFile zip = new MockMultipartFile("file", "archive.zip",
                "application/zip", "zipped".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.upload(workspaceId, zip, null, "GOOGLE", () -> "p"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(storageService, never()).upload(any(), any(), any(), anyLong());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void upload_fileTooLarge_returns400() {
        MultipartFile huge = mock(MultipartFile.class);
        when(huge.isEmpty()).thenReturn(false);
        when(huge.getOriginalFilename()).thenReturn("conclusion.pdf");
        when(huge.getContentType()).thenReturn("application/pdf");
        when(huge.getSize()).thenReturn(StyleCorpusCommandService.MAX_FILE_SIZE_BYTES + 1);

        assertThatThrownBy(() -> service.upload(workspaceId, huge, null, "GOOGLE", () -> "p"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(storageService, never()).upload(any(), any(), any(), anyLong());
    }

    @Test
    void upload_missingFile_returns400() {
        MultipartFile empty = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.upload(workspaceId, empty, null, "GOOGLE", () -> "p"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void upload_otherWorkspace_returns404() {
        UUID otherWorkspaceId = UUID.randomUUID();

        assertThatThrownBy(() -> service.upload(otherWorkspaceId, pdfFile(), null, "GOOGLE", () -> "p"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void upload_supportedByExtension_whenContentTypeGeneric() {
        MultipartFile docx = new MockMultipartFile("file", "conclusion.docx",
                "application/octet-stream", "docx bytes".getBytes(StandardCharsets.UTF_8));

        StyleCorpusUploadResponse response = service.upload(workspaceId, docx, null, "GOOGLE", () -> "p");

        assertThat(response.status()).isEqualTo(StyleCorpusDocumentStatus.PENDING);
    }

    @Test
    void list_returnsWorkspaceDocumentsWithoutSignature() {
        StyleCorpusDocument doc = new StyleCorpusDocument();
        ReflectionTestUtils.setField(doc, "id", UUID.randomUUID());
        doc.setOriginalFilename("conclusion.pdf");
        doc.setStatus(StyleCorpusDocumentStatus.DONE);
        doc.setActive(true);
        doc.setStyleSignature("SECRET — usage interne");
        when(styleCorpusRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId))
                .thenReturn(List.of(doc));

        List<StyleCorpusDocumentSummary> result = service.list(workspaceId, null, "GOOGLE", () -> "p");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).originalFilename()).isEqualTo("conclusion.pdf");
        // La signature de style n'est pas portée par le DTO.
        assertThat(result.get(0).toString()).doesNotContain("SECRET");
    }

    @Test
    void updateActive_togglesActiveFlag() {
        UUID docId = UUID.randomUUID();
        StyleCorpusDocument doc = new StyleCorpusDocument();
        ReflectionTestUtils.setField(doc, "id", docId);
        doc.setOriginalFilename("conclusion.pdf");
        doc.setStatus(StyleCorpusDocumentStatus.DONE);
        doc.setActive(true);
        when(styleCorpusRepository.findByIdAndWorkspaceId(docId, workspaceId))
                .thenReturn(Optional.of(doc));

        StyleCorpusDocumentSummary result = service.updateActive(
                workspaceId, docId, false, null, "GOOGLE", () -> "p");

        assertThat(result.active()).isFalse();
        assertThat(doc.isActive()).isFalse();
    }

    @Test
    void updateActive_missingActiveField_returns400() {
        assertThatThrownBy(() -> service.updateActive(
                workspaceId, UUID.randomUUID(), null, null, "GOOGLE", () -> "p"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateActive_unknownDocument_returns404() {
        UUID docId = UUID.randomUUID();
        when(styleCorpusRepository.findByIdAndWorkspaceId(docId, workspaceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateActive(
                workspaceId, docId, true, null, "GOOGLE", () -> "p"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_removesDocument() {
        UUID docId = UUID.randomUUID();
        StyleCorpusDocument doc = new StyleCorpusDocument();
        ReflectionTestUtils.setField(doc, "id", docId);
        when(styleCorpusRepository.findByIdAndWorkspaceId(docId, workspaceId))
                .thenReturn(Optional.of(doc));

        service.delete(workspaceId, docId, null, "GOOGLE", () -> "p");

        verify(styleCorpusRepository).delete(doc);
    }

    @Test
    void delete_unknownDocument_returns404() {
        UUID docId = UUID.randomUUID();
        when(styleCorpusRepository.findByIdAndWorkspaceId(docId, workspaceId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(workspaceId, docId, null, "GOOGLE", () -> "p"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    /** Helper de capture d'argument pour vérifier la ligne sauvegardée. */
    static final class ArgumentCaptorHolder {
        static <T> void verifySaved(StyleCorpusRepository repo, java.util.function.Consumer<T> assertion) {
            org.mockito.ArgumentCaptor<StyleCorpusDocument> captor =
                    org.mockito.ArgumentCaptor.forClass(StyleCorpusDocument.class);
            verify(repo, atLeastOnce()).save(captor.capture());
            @SuppressWarnings("unchecked")
            T saved = (T) captor.getValue();
            assertion.accept(saved);
        }
    }
}
