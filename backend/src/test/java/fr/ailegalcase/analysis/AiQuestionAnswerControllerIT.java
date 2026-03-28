package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class AiQuestionAnswerControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
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

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;
    private AiQuestion question;

    @BeforeEach
    void setUp() {
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
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("answer-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-answer-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("answer-test@example.com");
        workspace.setSlug("answer-slug-" + System.currentTimeMillis());
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
        caseFile.setTitle("Dossier Test Réponse");
        caseFile.setStatus("OPEN");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFileRepository.save(caseFile);

        question = new AiQuestion();
        question.setCaseFile(caseFile);
        question.setQuestionText("Question de test ?");
        question.setOrderIndex(0);
        aiQuestionRepository.save(question);

        auth = buildGoogleAuth("google-answer-sub", "answer-test@example.com");
    }

    // I-01 : POST réponse → 201, answer persisté, question ANSWERED
    @Test
    void answer_nominal_returns201AndPersistsAnswer() throws Exception {
        mockMvc.perform(post("/api/v1/ai-questions/{id}/answer", question.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\": \"Ma réponse complète\"}"))
                .andExpect(status().isCreated());

        var answers = aiQuestionAnswerRepository.findAll();
        assertThat(answers).hasSize(1);
        assertThat(answers.get(0).getAnswerText()).isEqualTo("Ma réponse complète");

        var updatedQuestion = aiQuestionRepository.findById(question.getId()).orElseThrow();
        assertThat(updatedQuestion.getStatus()).isEqualTo("ANSWERED");
        assertThat(updatedQuestion.getAnsweredAt()).isNotNull();
    }

    // I-02 : GET questions après réponse → answerText non null
    @Test
    void listQuestions_afterAnswer_returnsAnswerText() throws Exception {
        mockMvc.perform(post("/api/v1/ai-questions/{id}/answer", question.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\": \"Ma réponse\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/case-files/{id}/ai-questions", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].answerText").value("Ma réponse"))
                .andExpect(jsonPath("$[0].id").isNotEmpty());
    }

    // I-03 : POST réponse vide → 400
    @Test
    void answer_emptyText_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai-questions/{id}/answer", question.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    // I-04 : POST question inconnue → 404
    @Test
    void answer_unknownQuestion_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/ai-questions/{id}/answer", UUID.randomUUID())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\": \"réponse\"}"))
                .andExpect(status().isNotFound());
    }

    // I-05 : POST question autre workspace → 404
    @Test
    void answer_otherWorkspace_returns404() throws Exception {
        User otherUser = new User();
        otherUser.setEmail("other-answer@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-answer@example.com");
        otherWorkspace.setSlug("other-answer-slug-" + System.currentTimeMillis());
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

        AiQuestion otherQuestion = new AiQuestion();
        otherQuestion.setCaseFile(otherCaseFile);
        otherQuestion.setQuestionText("Question autre workspace ?");
        otherQuestion.setOrderIndex(0);
        aiQuestionRepository.save(otherQuestion);

        mockMvc.perform(post("/api/v1/ai-questions/{id}/answer", otherQuestion.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\": \"réponse\"}"))
                .andExpect(status().isNotFound());
    }

    // I-07 : POST sur question déjà ANSWERED → 201, 2 entrées en base, dernière réponse prioritaire
    @Test
    void answer_alreadyAnswered_creates2EntriesAndLatestIsReturned() throws Exception {
        mockMvc.perform(post("/api/v1/ai-questions/{id}/answer", question.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\": \"Première réponse\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/ai-questions/{id}/answer", question.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\": \"Réponse modifiée\"}"))
                .andExpect(status().isCreated());

        var answers = aiQuestionAnswerRepository.findAll();
        assertThat(answers).hasSize(2);

        var latest = aiQuestionAnswerRepository
                .findFirstByAiQuestionIdOrderByCreatedAtDesc(question.getId());
        assertThat(latest).isPresent();
        assertThat(latest.get().getAnswerText()).isEqualTo("Réponse modifiée");
    }

    // I-08 : garde SF-56-05 reconnaît la nouvelle réponse comme activité valide
    @Test
    void answer_reanswer_guardDetectsNewActivity() throws Exception {
        mockMvc.perform(post("/api/v1/ai-questions/{id}/answer", question.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\": \"Première réponse\"}"))
                .andExpect(status().isCreated());

        Instant afterFirst = Instant.now();

        mockMvc.perform(post("/api/v1/ai-questions/{id}/answer", question.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\": \"Réponse modifiée\"}"))
                .andExpect(status().isCreated());

        boolean hasNewActivity = aiQuestionAnswerRepository
                .existsByAiQuestion_CaseFile_IdAndCreatedAtAfter(caseFile.getId(), afterFirst);
        assertThat(hasNewActivity).isTrue();
    }

    // I-06 : POST sans auth → 401
    @Test
    void answer_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai-questions/{id}/answer", question.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\": \"réponse\"}"))
                .andExpect(status().isUnauthorized());
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
