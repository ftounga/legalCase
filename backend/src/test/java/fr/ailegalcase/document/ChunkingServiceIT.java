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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.microsoft.client-id=test-microsoft-id",
        "spring.security.oauth2.client.registration.microsoft.client-secret=test-microsoft-secret"
})
class ChunkingServiceIT {

    @Autowired private ChunkingService chunkingService;
    @Autowired private DocumentChunkRepository chunkRepository;
    @Autowired private DocumentExtractionRepository extractionRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;

    @MockBean private StorageService storageService;

    private UUID extractionId;

    @BeforeEach
    void setUp() {
        chunkRepository.deleteAll();
        extractionRepository.deleteAll();
        documentRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("chunk-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("chunk-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("chunk-test@example.com");
        workspace.setSlug("chunk-slug-" + System.currentTimeMillis());
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
        caseFile.setTitle("Dossier chunking test");
        caseFile.setLegalDomain("EMPLOYMENT_LAW");
        caseFile.setStatus("OPEN");
        caseFileRepository.save(caseFile);

        Document document = new Document();
        document.setCaseFile(caseFile);
        document.setUploadedBy(user);
        document.setOriginalFilename("test.txt");
        document.setContentType("text/plain");
        document.setFileSize(100L);
        document.setStorageKey("ws/cf/uuid/test.txt");
        documentRepository.save(document);

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setDocument(document);
        extraction.setExtractionStatus(ExtractionStatus.DONE);
        extractionRepository.save(extraction);
        extractionId = extraction.getId();
    }

    // C-01 : texte court → 1 chunk en base
    @Test
    void chunk_shortText_saves1ChunkToDb() {
        chunkingService.chunk(extractionId, "Contrat de travail. Licenciement abusif.");

        List<DocumentChunk> chunks = chunkRepository.findAll();
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getChunkIndex()).isEqualTo(0);
        assertThat(chunks.get(0).getChunkText()).contains("Contrat");
        assertThat(chunks.get(0).getTokenCount()).isPositive();
        assertThat(chunks.get(0).getChunkMetadata()).contains("startChar");
    }

    // C-02 : texte long → plusieurs chunks
    @Test
    void chunk_longText_savesMultipleChunks() {
        String text = "mot ".repeat(500); // ~2000 chars

        chunkingService.chunk(extractionId, text);

        List<DocumentChunk> chunks = chunkRepository.findAll();
        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks).isSortedAccordingTo(
                java.util.Comparator.comparingInt(DocumentChunk::getChunkIndex));
    }

    // C-03 : texte vide → aucun chunk
    @Test
    void chunk_emptyText_savesNoChunks() {
        chunkingService.chunk(extractionId, "");

        assertThat(chunkRepository.findAll()).isEmpty();
    }
}
