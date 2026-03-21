package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.DocumentChunkRepository;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.microsoft.client-id=test-microsoft-id",
        "spring.security.oauth2.client.registration.microsoft.client-secret=test-microsoft-secret"
})
@AutoConfigureMockMvc
class AiQuestionControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private AiQuestionRepository aiQuestionRepository;
    @Autowired private AnalysisJobRepository analysisJobRepository;
    @Autowired private CaseAnalysisRepository caseAnalysisRepository;
    @Autowired private DocumentAnalysisRepository documentAnalysisRepository;
    @Autowired private ChunkAnalysisRepository chunkAnalysisRepository;
    @Autowired private DocumentChunkRepository documentChunkRepository;
    @Autowired private DocumentExtractionRepository documentExtractionRepository;
    @Autowired private DocumentRepository documentRepository;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        aiQuestionRepository.deleteAll();
        chunkAnalysisRepository.deleteAll();
        documentChunkRepository.deleteAll();
        documentAnalysisRepository.deleteAll();
        documentExtractionRepository.deleteAll();
        analysisJobRepository.deleteAll();
        caseAnalysisRepository.deleteAll();
        documentRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("questions-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-questions-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("questions-test@example.com");
        workspace.setSlug("questions-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
       workspace.setLegalDomain("DROIT_DU_TRAVAIL");

        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFile.setTitle("Dossier Test Questions");
        caseFile.setStatus("OPEN");
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-questions-sub", "questions-test@example.com");
    }

    // I-01 : GET questions → 200 avec liste triée par orderIndex
    @Test
    void list_withQuestions_returns200AndOrderedList() throws Exception {
        AiQuestion q1 = buildQuestion(caseFile, 0, "Question A ?");
        AiQuestion q2 = buildQuestion(caseFile, 1, "Question B ?");
        aiQuestionRepository.saveAll(List.of(q1, q2));

        mockMvc.perform(get("/api/v1/case-files/{id}/ai-questions", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].orderIndex").value(0))
                .andExpect(jsonPath("$[0].questionText").value("Question A ?"))
                .andExpect(jsonPath("$[1].orderIndex").value(1))
                .andExpect(jsonPath("$[1].questionText").value("Question B ?"));
    }

    // I-02 : GET sans questions → 200 liste vide
    @Test
    void list_noQuestions_returns200EmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/ai-questions", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // I-03 : GET dossier inconnu → 404
    @Test
    void list_unknownCaseFile_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/ai-questions", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-04 : GET dossier d'un autre workspace → 404 (isolation workspace)
    @Test
    void list_otherWorkspaceCaseFile_returns404() throws Exception {
        User otherUser = new User();
        otherUser.setEmail("other-questions@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-other-questions-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-questions@example.com");
        otherWorkspace.setSlug("other-questions-slug-" + System.currentTimeMillis());
        otherWorkspace.setOwner(otherUser);
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        workspaceRepository.save(otherWorkspace);

        WorkspaceMember otherMember = new WorkspaceMember();
        otherMember.setWorkspace(otherWorkspace);
        otherMember.setUser(otherUser);
        otherMember.setMemberRole("OWNER");
        otherMember.setPrimary(true);
        otherMember.setPrimary(true);
        workspaceMemberRepository.save(otherMember);

        CaseFile otherCaseFile = new CaseFile();
        otherCaseFile.setWorkspace(otherWorkspace);
        otherCaseFile.setCreatedBy(otherUser);
        otherCaseFile.setTitle("Dossier Autre Workspace");
        otherCaseFile.setStatus("OPEN");
        caseFileRepository.save(otherCaseFile);

        mockMvc.perform(get("/api/v1/case-files/{id}/ai-questions", otherCaseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-05 : GET sans auth → 401
    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/ai-questions", caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    private AiQuestion buildQuestion(CaseFile cf, int orderIndex, String text) {
        AiQuestion q = new AiQuestion();
        q.setCaseFile(cf);
        q.setOrderIndex(orderIndex);
        q.setQuestionText(text);
        return q;
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
