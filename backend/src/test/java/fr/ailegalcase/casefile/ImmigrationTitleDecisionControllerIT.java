package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ImmigrationTitleDecisionControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authImmigration;
    private OAuth2AuthenticationToken authOther;
    private CaseFile immigrationCaseFile;
    private CaseFile travailCaseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Immigration workspace + case file
        User userImm = new User();
        userImm.setEmail("td-imm-" + ts + "@example.com");
        userImm.setStatus("ACTIVE");
        userImm = userRepository.save(userImm);

        AuthAccount accImm = new AuthAccount();
        accImm.setUser(userImm);
        accImm.setProvider("GOOGLE");
        accImm.setProviderUserId("google-td-imm-" + ts);
        authAccountRepository.save(accImm);

        Workspace wsImm = new Workspace();
        wsImm.setName("Cabinet Immigration " + ts);
        wsImm.setSlug("ws-imm-" + ts);
        wsImm.setOwner(userImm);
        wsImm.setLegalDomain("DROIT_IMMIGRATION");
        wsImm.setCountry("FRANCE");
        wsImm.setPlanCode("STARTER");
        wsImm.setStatus("ACTIVE");
        wsImm = workspaceRepository.save(wsImm);

        WorkspaceMember memberImm = new WorkspaceMember();
        memberImm.setWorkspace(wsImm);
        memberImm.setUser(userImm);
        memberImm.setMemberRole("OWNER");
        memberImm.setPrimary(true);
        workspaceMemberRepository.save(memberImm);

        immigrationCaseFile = new CaseFile();
        immigrationCaseFile.setTitle("Dossier immigration " + ts);
        immigrationCaseFile.setWorkspace(wsImm);
        immigrationCaseFile.setCreatedBy(userImm);
        immigrationCaseFile.setLegalDomain("DROIT_IMMIGRATION");
        immigrationCaseFile.setStatus("OPEN");
        immigrationCaseFile = caseFileRepository.save(immigrationCaseFile);

        authImmigration = buildGoogleAuth("google-td-imm-" + ts, "td-imm-" + ts + "@example.com");

        // Other workspace (droit du travail)
        User userOther = new User();
        userOther.setEmail("td-other-" + ts + "@example.com");
        userOther.setStatus("ACTIVE");
        userOther = userRepository.save(userOther);

        AuthAccount accOther = new AuthAccount();
        accOther.setUser(userOther);
        accOther.setProvider("GOOGLE");
        accOther.setProviderUserId("google-td-other-" + ts);
        authAccountRepository.save(accOther);

        Workspace wsOther = new Workspace();
        wsOther.setName("Cabinet Travail " + ts);
        wsOther.setSlug("ws-other-" + ts);
        wsOther.setOwner(userOther);
        wsOther.setLegalDomain("DROIT_DU_TRAVAIL");
        wsOther.setCountry("FRANCE");
        wsOther.setPlanCode("STARTER");
        wsOther.setStatus("ACTIVE");
        wsOther = workspaceRepository.save(wsOther);

        WorkspaceMember memberOther = new WorkspaceMember();
        memberOther.setWorkspace(wsOther);
        memberOther.setUser(userOther);
        memberOther.setMemberRole("OWNER");
        memberOther.setPrimary(true);
        workspaceMemberRepository.save(memberOther);

        travailCaseFile = new CaseFile();
        travailCaseFile.setTitle("Dossier travail " + ts);
        travailCaseFile.setWorkspace(wsOther);
        travailCaseFile.setCreatedBy(userOther);
        travailCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        travailCaseFile.setStatus("OPEN");
        travailCaseFile = caseFileRepository.save(travailCaseFile);

        authOther = buildGoogleAuth("google-td-other-" + ts, "td-other-" + ts + "@example.com");
    }

    @Test
    void POST_resolve_returns200_withRecommendations() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "country", "FRANCE",
                "nationaliteUe", false,
                "motif", "TRAVAIL",
                "duree", "LONG_SEJOUR"
        ));

        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/title-decision")
                        .with(authentication(authImmigration))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseFileId").value(immigrationCaseFile.getId().toString()))
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.motif").value("TRAVAIL"))
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations[0].code").value("VLS_TS_SALARIE"));
    }

    @Test
    void POST_resolve_belgique_returns200() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "country", "BELGIQUE",
                "nationaliteUe", false,
                "motif", "ETUDES",
                "duree", "COURT_SEJOUR"
        ));

        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/title-decision")
                        .with(authentication(authImmigration))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.recommendations[0].code").value("CARTE_A_ETUDES"));
    }

    @Test
    void POST_invalidCountry_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "country", "ALLEMAGNE",
                "nationaliteUe", false,
                "motif", "TRAVAIL",
                "duree", "LONG_SEJOUR"
        ));

        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/title-decision")
                        .with(authentication(authImmigration))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_wrongDomain_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "country", "FRANCE",
                "nationaliteUe", false,
                "motif", "TRAVAIL",
                "duree", "LONG_SEJOUR"
        ));

        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/immigration/title-decision")
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "country", "FRANCE",
                "nationaliteUe", false,
                "motif", "TRAVAIL",
                "duree", "LONG_SEJOUR"
        ));

        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/title-decision")
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_afterPost_returns200() throws Exception {
        // First POST to create a decision
        String body = objectMapper.writeValueAsString(Map.of(
                "country", "FRANCE",
                "nationaliteUe", false,
                "motif", "FAMILLE",
                "duree", "COURT_SEJOUR",
                "situationFamiliale", "MARIE"
        ));

        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/title-decision")
                        .with(authentication(authImmigration))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Then GET
        mockMvc.perform(get("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/title-decision")
                        .with(authentication(authImmigration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.motif").value("FAMILLE"))
                .andExpect(jsonPath("$.situationFamiliale").value("MARIE"))
                .andExpect(jsonPath("$.recommendations[0].code").value("CST_VPF"));
    }

    @Test
    void GET_withoutPriorPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/title-decision")
                        .with(authentication(authImmigration)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesExistingDecision() throws Exception {
        // First decision: TRAVAIL
        String body1 = objectMapper.writeValueAsString(Map.of(
                "country", "FRANCE",
                "nationaliteUe", false,
                "motif", "TRAVAIL",
                "duree", "LONG_SEJOUR"
        ));

        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/title-decision")
                        .with(authentication(authImmigration))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motif").value("TRAVAIL"));

        // Second decision: ETUDES — should replace
        String body2 = objectMapper.writeValueAsString(Map.of(
                "country", "BELGIQUE",
                "nationaliteUe", false,
                "motif", "ETUDES",
                "duree", "COURT_SEJOUR"
        ));

        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/title-decision")
                        .with(authentication(authImmigration))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.motif").value("ETUDES"))
                .andExpect(jsonPath("$.recommendations[0].code").value("CARTE_A_ETUDES"));

        // GET should return the second decision
        mockMvc.perform(get("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/title-decision")
                        .with(authentication(authImmigration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motif").value("ETUDES"));
    }

    // ---- F-IM-18 SF-IM-18-01 : situation familiale branchée ----

    @Test
    void POST_famille_celibataire_longSejour_france_returnsOnlyCstVpf() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "country", "FRANCE",
                "nationaliteUe", false,
                "motif", "FAMILLE",
                "duree", "LONG_SEJOUR",
                "situationFamiliale", "CELIBATAIRE"
        ));

        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/title-decision")
                        .with(authentication(authImmigration))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situationFamiliale").value("CELIBATAIRE"))
                .andExpect(jsonPath("$.recommendations.length()").value(1))
                .andExpect(jsonPath("$.recommendations[0].code").value("CST_VPF"));
    }

    @Test
    void POST_famille_marie_longSejour_france_includesCarteResident() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "country", "FRANCE",
                "nationaliteUe", false,
                "motif", "FAMILLE",
                "duree", "LONG_SEJOUR",
                "situationFamiliale", "MARIE"
        ));

        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/title-decision")
                        .with(authentication(authImmigration))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations.length()").value(2))
                .andExpect(jsonPath("$.recommendations[0].code").value("CST_VPF"))
                .andExpect(jsonPath("$.recommendations[1].code").value("CARTE_RESIDENT"));
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
