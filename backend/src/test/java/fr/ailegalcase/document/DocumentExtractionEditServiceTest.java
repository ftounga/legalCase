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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentExtractionEditServiceTest {

    @Mock DocumentRepository documentRepository;
    @Mock DocumentExtractionRepository extractionRepository;
    @Mock CaseFileRepository caseFileRepository;
    @Mock WorkspaceMemberRepository workspaceMemberRepository;
    @Mock CurrentUserResolver currentUserResolver;

    @InjectMocks DocumentExtractionEditService service;

    UUID workspaceId = UUID.randomUUID();
    UUID caseFileId = UUID.randomUUID();
    UUID documentId = UUID.randomUUID();
    DocumentExtraction extraction;

    @BeforeEach
    void setup() {
        User user = new User();
        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);

        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);
        caseFile.setWorkspace(workspace);

        Document document = new Document();
        document.setId(documentId);
        document.setCaseFile(caseFile);

        extraction = new DocumentExtraction();
        extraction.setDocument(document);
        extraction.setExtractionStatus(ExtractionStatus.DONE);
        extraction.setExtractedText("texte original OCR");

        lenient().when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        lenient().when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        lenient().when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        lenient().when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        lenient().when(extractionRepository.findByDocumentId(documentId)).thenReturn(Optional.of(extraction));
        lenient().when(extractionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void editText_firstEdit_backsUpOriginalAndStoresNewText() {
        service.editText(caseFileId, documentId, "texte corrigé par l'avocat", null, null, null);

        assertThat(extraction.getExtractedText()).isEqualTo("texte corrigé par l'avocat");
        assertThat(extraction.getExtractedTextOriginal()).isEqualTo("texte original OCR");
        assertThat(extraction.getTextEditedAt()).isNotNull();
    }

    @Test
    void editText_secondEdit_preservesOriginalBackup() {
        extraction.setExtractedText("1er edit");
        extraction.setExtractedTextOriginal("texte original OCR");

        service.editText(caseFileId, documentId, "2e edit", null, null, null);

        assertThat(extraction.getExtractedText()).isEqualTo("2e edit");
        assertThat(extraction.getExtractedTextOriginal()).isEqualTo("texte original OCR");
    }

    @Test
    void editText_nullText_throws400() {
        assertThatThrownBy(() -> service.editText(caseFileId, documentId, null, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("extractedText is required");
    }

    @Test
    void editText_statusNotDone_throws409() {
        extraction.setExtractionStatus(ExtractionStatus.PENDING);

        assertThatThrownBy(() -> service.editText(caseFileId, documentId, "x", null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot edit");
    }

    @Test
    void resetToOriginal_restoresOriginalAndClearsBackup() {
        extraction.setExtractedText("edited");
        extraction.setExtractedTextOriginal("texte original OCR");
        extraction.setTextEditedAt(java.time.Instant.now());

        service.resetToOriginal(caseFileId, documentId, null, null, null);

        assertThat(extraction.getExtractedText()).isEqualTo("texte original OCR");
        assertThat(extraction.getExtractedTextOriginal()).isNull();
        assertThat(extraction.getTextEditedAt()).isNull();
    }

    @Test
    void resetToOriginal_neverEdited_throws409() {
        assertThatThrownBy(() -> service.resetToOriginal(caseFileId, documentId, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("nothing to reset");
    }
}
