package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.chat.ChatMessage;
import fr.ailegalcase.chat.ChatMessageRepository;
import fr.ailegalcase.analysis.UsageEvent;
import fr.ailegalcase.analysis.UsageEventRepository;
import fr.ailegalcase.document.DocumentChunkRepository;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class ReAnalysisControllerIT {

    @MockBean private RabbitTemplate rabbitTemplate;

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private AiQuestionRepository aiQuestionRepository;
    @Autowired private AiQuestionAnswerRepository aiQuestionAnswerRepository;
    @Autowired private AnalysisJobRepository analysisJobRepository;
    @Autowired private CaseAnalysisRepository caseAnalysisRepository;
    @Autowired private DocumentAnalysisRepository documentAnalysisRepository;
    @Autowired private ChunkAnalysisRepository chunkAnalysisRepository;
    @Autowired private DocumentChunkRepository documentChunkRepository;
    @Autowired private DocumentExtractionRepository documentExtractionRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private UsageEventRepository usageEventRepository;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;
    private Workspace workspace;
    private User testUser;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
        usageEventRepository.deleteAll();
        aiQuestionAnswerRepository.deleteAll();
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
        subscriptionRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("reanalysis-test@example.com");
        testUser.setStatus("ACTIVE");
        userRepository.save(testUser);
        User user = testUser;

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-reanalysis-sub");
        authAccountRepository.save(account);

        workspace = new Workspace();
        workspace.setName("reanalysis-test@example.com");
        workspace.setSlug("reanalysis-slug-" + System.currentTimeMillis());
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
        caseFile.setTitle("Dossier Test Re-analyse");
        caseFile.setStatus("OPEN");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-reanalysis-sub", "reanalysis-test@example.com");
    }

    // I-01 : POST re-analyze → 202, job ENRICHED_ANALYSIS créé à PROCESSING
    @Test
    void reAnalyze_nominal_returns202AndCreatesJob() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/re-analyze", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isAccepted());

        var jobs = analysisJobRepository.findByCaseFileId(caseFile.getId());
        assertThat(jobs).anyMatch(j ->
                j.getJobType() == JobType.ENRICHED_ANALYSIS
                && j.getStatus() == AnalysisStatus.PROCESSING);
    }

    // I-02 : POST dossier inconnu → 404
    @Test
    void reAnalyze_unknownCaseFile_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/re-analyze", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-03 : POST dossier autre workspace → 404
    @Test
    void reAnalyze_otherWorkspace_returns404() throws Exception {
        User otherUser = new User();
        otherUser.setEmail("other-reanalysis@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-reanalysis@example.com");
        otherWorkspace.setSlug("other-reanalysis-slug-" + System.currentTimeMillis());
        otherWorkspace.setOwner(otherUser);
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        otherWorkspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspaceRepository.save(otherWorkspace);

        CaseFile otherCaseFile = new CaseFile();
        otherCaseFile.setWorkspace(otherWorkspace);
        otherCaseFile.setCreatedBy(otherUser);
        otherCaseFile.setTitle("Dossier Autre");
        otherCaseFile.setStatus("OPEN");
        otherCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFileRepository.save(otherCaseFile);

        mockMvc.perform(post("/api/v1/case-files/{id}/re-analyze", otherCaseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-04 : POST sans auth → 401
    @Test
    void reAnalyze_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/re-analyze", caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    // I-05 : plan STARTER, limite de 1 re-analyse atteinte → 402
    @Test
    void reAnalyze_starterPlanLimitReached_returns402() throws Exception {
        Subscription subscription = new Subscription();
        subscription.setWorkspaceId(workspace.getId());
        subscription.setPlanCode("STARTER");
        subscription.setStatus("ACTIVE");
        subscription.setStartedAt(Instant.now());
        subscriptionRepository.save(subscription);

        // Simule 1 re-analyse déjà consommée (FREE_MAX_RE_ANALYSES_PER_CASE_FILE = 1)
        UsageEvent event = new UsageEvent();
        event.setCaseFileId(caseFile.getId());
        event.setUserId(testUser.getId());
        event.setEventType(JobType.ENRICHED_ANALYSIS);
        event.setTokensInput(100);
        event.setTokensOutput(50);
        event.setEstimatedCost(java.math.BigDecimal.ZERO);
        usageEventRepository.save(event);

        mockMvc.perform(post("/api/v1/case-files/{id}/re-analyze", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isPaymentRequired());
    }

    // I-06 : plan PRO avec subscription → 202
    @Test
    void reAnalyze_proPlan_returns202() throws Exception {
        Subscription subscription = new Subscription();
        subscription.setWorkspaceId(workspace.getId());
        subscription.setPlanCode("PRO");
        subscription.setStatus("ACTIVE");
        subscription.setStartedAt(Instant.now());
        subscriptionRepository.save(subscription);

        mockMvc.perform(post("/api/v1/case-files/{id}/re-analyze", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isAccepted());
    }

    // I-07 : analyse enrichie précédente, nouveau message chat (sans réponse Q&A) → 202
    @Test
    void reAnalyze_newChatMessageOnly_returns202() throws Exception {
        CaseAnalysis lastEnriched = new CaseAnalysis();
        lastEnriched.setCaseFile(caseFile);
        lastEnriched.setVersion(1);
        lastEnriched.setAnalysisType(AnalysisType.ENRICHED);
        lastEnriched.setAnalysisStatus(AnalysisStatus.DONE);
        lastEnriched.setAnalysisResult("{}");
        caseAnalysisRepository.save(lastEnriched);

        ChatMessage msg = new ChatMessage();
        msg.setCaseFileId(caseFile.getId());
        msg.setUserId(testUser.getId());
        msg.setQuestion("Quelle est la prescription ?");
        msg.setAnswer("2 ans.");
        msg.setUseEnriched(false);
        chatMessageRepository.save(msg);

        mockMvc.perform(post("/api/v1/case-files/{id}/re-analyze", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isAccepted());
    }

    // I-08 : analyse enrichie précédente, aucun nouveau message chat ni réponse Q&A → 409
    @Test
    void reAnalyze_noNewChatNorAnswer_returns409() throws Exception {
        CaseAnalysis lastEnriched = new CaseAnalysis();
        lastEnriched.setCaseFile(caseFile);
        lastEnriched.setVersion(1);
        lastEnriched.setAnalysisType(AnalysisType.ENRICHED);
        lastEnriched.setAnalysisStatus(AnalysisStatus.DONE);
        lastEnriched.setAnalysisResult("{}");
        caseAnalysisRepository.save(lastEnriched);

        // Aucun message chat, aucune réponse Q&A

        mockMvc.perform(post("/api/v1/case-files/{id}/re-analyze", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isConflict());
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
