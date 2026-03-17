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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.microsoft.client-id=test-microsoft-id",
        "spring.security.oauth2.client.registration.microsoft.client-secret=test-microsoft-secret"
})
@AutoConfigureMockMvc
class DocumentControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private DocumentRepository documentRepository;

    @MockBean private StorageService storageService;

    private OAuth2AuthenticationToken auth;
    private UUID caseFileId;
    private UUID otherCaseFileId;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        // User + workspace
        User user = new User();
        user.setEmail("doc-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-doc-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("doc-test@example.com");
        workspace.setSlug("doc-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        workspaceMemberRepository.save(member);

        // Case file belonging to this workspace
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFile.setTitle("Dossier test upload");
        caseFile.setLegalDomain("EMPLOYMENT_LAW");
        caseFile.setStatus("OPEN");
        caseFileRepository.save(caseFile);
        caseFileId = caseFile.getId();

        // Case file in another workspace
        User otherUser = new User();
        otherUser.setEmail("other@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other@example.com");
        otherWorkspace.setSlug("other-slug-" + System.currentTimeMillis());
        otherWorkspace.setOwner(otherUser);
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        workspaceRepository.save(otherWorkspace);

        CaseFile otherCaseFile = new CaseFile();
        otherCaseFile.setWorkspace(otherWorkspace);
        otherCaseFile.setCreatedBy(otherUser);
        otherCaseFile.setTitle("Dossier autre workspace");
        otherCaseFile.setLegalDomain("EMPLOYMENT_LAW");
        otherCaseFile.setStatus("OPEN");
        caseFileRepository.save(otherCaseFile);
        otherCaseFileId = otherCaseFile.getId();

        auth = buildGoogleAuth("google-doc-sub", "doc-test@example.com");

        when(storageService.upload(anyString(), any(), anyString(), anyLong()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // I-01 : upload PDF valide → 201
    @Test
    void upload_validPdf_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.originalFilename").value("contrat.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.caseFileId").value(caseFileId.toString()));
    }

    // I-02 : type non supporté → 400
    @Test
    void upload_unsupportedType_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", "image data".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-03 : fichier vide → 400
    @Test
    void upload_emptyFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-04 : sans auth → 401
    @Test
    void upload_withoutAuth_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    // I-05 : case file inconnu → 404
    @Test
    void upload_unknownCaseFile_returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + UUID.randomUUID() + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-06 : case file d'un autre workspace → 404 (isolation)
    @Test
    void upload_caseFileBelongsToOtherWorkspace_returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + otherCaseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-07 : upload DOCX valide → 201
    @Test
    void upload_validDocx_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "courrier.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "DOCX content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("courrier.docx"));
    }

    // I-08 : GET /{docId}/download → 302 avec Location header
    @Test
    void download_existingDoc_returns302() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat.pdf", "application/pdf", "PDF content".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file).with(authentication(auth)))
                .andReturn().getResponse().getContentAsString();

        String docId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(uploadResponse).get("id").asText();

        when(storageService.presignedDownloadUrl(anyString(), any(Integer.class)))
                .thenReturn("http://minio:9000/legalcase-dev/some-key?X-Amz-Signature=abc");

        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents/" + docId + "/download")
                        .with(authentication(auth)))
                .andExpect(status().isFound())
                .andExpect(header().exists("Location"));
    }

    // I-09 : download sans auth → 401
    @Test
    void download_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents/" + UUID.randomUUID() + "/download"))
                .andExpect(status().isUnauthorized());
    }

    // I-10 : download doc inconnu → 404
    @Test
    void download_unknownDoc_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents/" + UUID.randomUUID() + "/download")
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-11 : GET liste vide → []
    @Test
    void list_noDocs_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // I-09 : GET liste avec docs → retourne les documents
    @Test
    void list_withDocs_returnsItems() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file).with(authentication(auth)));

        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].originalFilename").value("contrat.pdf"))
                .andExpect(jsonPath("$[0].contentType").value("application/pdf"))
                .andExpect(jsonPath("$[0].caseFileId").value(caseFileId.toString()));
    }

    // I-10 : GET liste sans auth → 401
    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents"))
                .andExpect(status().isUnauthorized());
    }

    // I-11 : GET liste — dossier d'un autre workspace → 404
    @Test
    void list_otherWorkspaceCaseFile_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + otherCaseFileId + "/documents")
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    private OAuth2AuthenticationToken buildGoogleAuth(String sub, String email) {
        Map<String, Object> claims = Map.of(
                "sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
