package fr.ailegalcase.document;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.storage.StorageService;
import fr.ailegalcase.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.microsoft.client-id=test-microsoft-id",
        "spring.security.oauth2.client.registration.microsoft.client-secret=test-microsoft-secret"
})
class ExtractionServiceIT {

    @Autowired private ExtractionService extractionService;
    @Autowired private DocumentExtractionRepository extractionRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;

    @MockBean private StorageService storageService;

    private UUID documentId;
    private String storageKey;

    @BeforeEach
    void setUp() {
        extractionRepository.deleteAll();
        documentRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("extraction-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("extraction-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("extraction-test@example.com");
        workspace.setSlug("extraction-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        workspaceMemberRepository.save(member);

        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFile.setTitle("Dossier extraction test");
        caseFile.setLegalDomain("EMPLOYMENT_LAW");
        caseFile.setStatus("OPEN");
        caseFileRepository.save(caseFile);

        Document document = new Document();
        document.setCaseFile(caseFile);
        document.setUploadedBy(user);
        document.setOriginalFilename("test.txt");
        document.setContentType("text/plain");
        document.setFileSize(100L);
        storageKey = workspace.getId() + "/" + caseFile.getId() + "/uuid/test.txt";
        document.setStorageKey(storageKey);
        documentRepository.save(document);
        documentId = document.getId();
    }

    // E-01 : extraction TXT réussie → status DONE, texte non vide
    @Test
    void extract_validTxt_createsDoneExtraction() {
        String content = "Contrat de travail. Monsieur Dupont est licencié.";
        when(storageService.download(anyString()))
                .thenReturn(content.getBytes(StandardCharsets.UTF_8));

        extractionService.extract(documentId, storageKey, "text/plain");

        Optional<DocumentExtraction> result = extractionRepository.findByDocumentId(documentId);
        assertThat(result).isPresent();
        assertThat(result.get().getExtractionStatus()).isEqualTo(ExtractionStatus.DONE);
        assertThat(result.get().getExtractedText()).contains("Dupont");
        assertThat(result.get().getExtractionMetadata()).contains("charCount");
    }

    // E-02 : échec download → status FAILED
    @Test
    void extract_storageFailure_createsFailedExtraction() {
        when(storageService.download(anyString()))
                .thenThrow(new RuntimeException("Storage unavailable"));

        extractionService.extract(documentId, storageKey, "text/plain");

        Optional<DocumentExtraction> result = extractionRepository.findByDocumentId(documentId);
        assertThat(result).isPresent();
        assertThat(result.get().getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(result.get().getExtractionMetadata()).contains("error");
    }

    // E-03 : type non supporté → status FAILED
    @Test
    void extract_unsupportedContentType_createsFailedExtraction() {
        when(storageService.download(anyString()))
                .thenReturn("some bytes".getBytes());

        extractionService.extract(documentId, storageKey, "image/png");

        Optional<DocumentExtraction> result = extractionRepository.findByDocumentId(documentId);
        assertThat(result).isPresent();
        assertThat(result.get().getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
    }
}
