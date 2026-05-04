package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F-160 SF-160-01 — garantit que l'historique des analyses (case_analyses) expose
 * les checklists procédurales (F-96) et questions IA (F-13/F-14) **par version**.
 *
 * <p>Aucun changement de code applicatif. Ce test fige le comportement existant
 * pour qu'il ne régresse pas.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class SynthesisVersioningIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private CaseAnalysisRepository caseAnalysisRepository;
    @Autowired private ProcedureCheckRepository procedureCheckRepository;
    @Autowired private AiQuestionRepository aiQuestionRepository;
    @Autowired private AnalysisJobRepository analysisJobRepository;
    @Autowired private DocumentAnalysisRepository documentAnalysisRepository;
    @Autowired private ChunkAnalysisRepository chunkAnalysisRepository;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;
    private Workspace workspace;
    private CaseAnalysis analysisV1;
    private CaseAnalysis analysisV2;

    @BeforeEach
    void setUp() {
        procedureCheckRepository.deleteAll();
        aiQuestionRepository.deleteAll();
        chunkAnalysisRepository.deleteAll();
        documentAnalysisRepository.deleteAll();
        analysisJobRepository.deleteAll();
        caseAnalysisRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("synth-versioning@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-synth-sub");
        authAccountRepository.save(account);

        workspace = new Workspace();
        workspace.setName("synth-versioning@example.com");
        workspace.setSlug("synth-versioning-slug-" + System.currentTimeMillis());
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
        workspaceMemberRepository.save(member);

        caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFile.setTitle("Dossier Versioning Synthese");
        caseFile.setStatus("OPEN");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFileRepository.save(caseFile);

        // F-160 : 2 itérations STANDARD (pattern re-analyse). On évite la paire
        // STANDARD → ENRICHED ici car listQuestionsByAnalysisId applique un
        // fallback ENRICHED → preceding STANDARD (design F-56) qui sort du
        // périmètre per-iteration F-160.
        analysisV1 = saveAnalysis(1, AnalysisType.STANDARD,
                "{\"faits\":[{\"texte\":\"Fait V1\"}],\"timeline\":[],\"risques\":[],\"points_juridiques\":[]}");
        analysisV2 = saveAnalysis(2, AnalysisType.STANDARD,
                "{\"faits\":[{\"texte\":\"Fait V2\"},{\"texte\":\"Fait V2 bis\"}],\"timeline\":[],\"risques\":[],\"points_juridiques\":[]}");

        // 1 procedure_check par version, libellés différents
        procedureCheckRepository.save(buildCheck(analysisV1, 0, "Vérifier convocation V1"));
        procedureCheckRepository.save(buildCheck(analysisV2, 0, "Vérifier convocation V2"));
        procedureCheckRepository.save(buildCheck(analysisV2, 1, "Vérifier délai V2"));

        // 1 question par version, textes différents
        aiQuestionRepository.save(buildQuestion(0, "Question V1 ?", analysisV1));
        aiQuestionRepository.save(buildQuestion(0, "Question V2 ?", analysisV2));

        auth = buildGoogleAuth("google-synth-sub", "synth-versioning@example.com");
    }

    @Test
    void listVersions_returns2VersionsDescending() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/case-analysis/versions", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByVersion_v1_returnsAnalysisV1Id() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/case-files/{id}/case-analysis/versions/{v}", caseFile.getId(), 1)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        assertThat(root.get("id").asText()).isEqualTo(analysisV1.getId().toString());
    }

    @Test
    void getByVersion_v2_returnsAnalysisV2Id() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/case-files/{id}/case-analysis/versions/{v}", caseFile.getId(), 2)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        assertThat(root.get("id").asText()).isEqualTo(analysisV2.getId().toString());
    }

    @Test
    void procedureChecksByAnalysisV1_returnsV1ChecksOnly() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{cf}/analyses/{a}/procedure-checks",
                        caseFile.getId(), analysisV1.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Vérifier convocation V1"));
    }

    @Test
    void procedureChecksByAnalysisV2_returnsV2ChecksOnly() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{cf}/analyses/{a}/procedure-checks",
                        caseFile.getId(), analysisV2.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].description").value("Vérifier convocation V2"));
    }

    @Test
    void aiQuestionsByAnalysisV1_returnsV1QuestionOnly() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{cf}/ai-questions", caseFile.getId())
                        .param("analysisId", analysisV1.getId().toString())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].questionText").value("Question V1 ?"));
    }

    @Test
    void aiQuestionsByAnalysisV2_returnsV2QuestionOnly() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{cf}/ai-questions", caseFile.getId())
                        .param("analysisId", analysisV2.getId().toString())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].questionText").value("Question V2 ?"));
    }

    private CaseAnalysis saveAnalysis(int version, AnalysisType type, String result) {
        CaseAnalysis a = new CaseAnalysis();
        a.setCaseFile(caseFile);
        a.setVersion(version);
        a.setAnalysisType(type);
        a.setAnalysisStatus(AnalysisStatus.DONE);
        a.setAnalysisResult(result);
        return caseAnalysisRepository.save(a);
    }

    private ProcedureCheck buildCheck(CaseAnalysis analysis, int ordre, String description) {
        ProcedureCheck c = new ProcedureCheck();
        c.setCaseAnalysis(analysis);
        c.setWorkspace(workspace);
        c.setOrdre(ordre);
        c.setDescription(description);
        c.setStatut(ProcedureCheckStatus.TO_CHECK);
        return c;
    }

    private AiQuestion buildQuestion(int orderIndex, String text, CaseAnalysis analysis) {
        AiQuestion q = new AiQuestion();
        q.setCaseFile(caseFile);
        q.setCaseAnalysis(analysis);
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
