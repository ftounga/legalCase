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
class ImmigrationWorkRightControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authImm;
    private OAuth2AuthenticationToken authOther;
    private CaseFile immCaseFile;
    private CaseFile travailCaseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User userImm = new User();
        userImm.setEmail("wr-imm-" + ts + "@example.com");
        userImm.setStatus("ACTIVE");
        userImm = userRepository.save(userImm);

        AuthAccount accImm = new AuthAccount();
        accImm.setUser(userImm);
        accImm.setProvider("GOOGLE");
        accImm.setProviderUserId("google-wr-imm-" + ts);
        authAccountRepository.save(accImm);

        Workspace wsImm = new Workspace();
        wsImm.setName("WR Imm " + ts);
        wsImm.setSlug("ws-wr-imm-" + ts);
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

        immCaseFile = new CaseFile();
        immCaseFile.setTitle("Dossier WR " + ts);
        immCaseFile.setWorkspace(wsImm);
        immCaseFile.setCreatedBy(userImm);
        immCaseFile.setLegalDomain("DROIT_IMMIGRATION");
        immCaseFile.setStatus("OPEN");
        immCaseFile = caseFileRepository.save(immCaseFile);

        authImm = buildGoogleAuth("google-wr-imm-" + ts, "wr-imm-" + ts + "@example.com");

        User userOther = new User();
        userOther.setEmail("wr-other-" + ts + "@example.com");
        userOther.setStatus("ACTIVE");
        userOther = userRepository.save(userOther);

        AuthAccount accOther = new AuthAccount();
        accOther.setUser(userOther);
        accOther.setProvider("GOOGLE");
        accOther.setProviderUserId("google-wr-other-" + ts);
        authAccountRepository.save(accOther);

        Workspace wsOther = new Workspace();
        wsOther.setName("WR Other " + ts);
        wsOther.setSlug("ws-wr-other-" + ts);
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

        authOther = buildGoogleAuth("google-wr-other-" + ts, "wr-other-" + ts + "@example.com");
    }

    @Test
    void POST_france_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immCaseFile.getId() + "/immigration/work-right")
                        .with(authentication(authImm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("titreType", "VLS_TS_SALARIE", "country", "FRANCE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.droitTravail").value("OUI"))
                .andExpect(jsonPath("$.obligationsEmployeur").isArray());
    }

    @Test
    void POST_belgique_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immCaseFile.getId() + "/immigration/work-right")
                        .with(authentication(authImm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("titreType", "CARTE_A_ETUDES", "country", "BELGIQUE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.droitTravail").value("CONDITIONNEL"))
                .andExpect(jsonPath("$.conditions").isNotEmpty());
    }

    @Test
    void POST_unknownTitre_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immCaseFile.getId() + "/immigration/work-right")
                        .with(authentication(authImm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("titreType", "INCONNU", "country", "FRANCE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_wrongDomain_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/immigration/work-right")
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("titreType", "VLS_TS_SALARIE", "country", "FRANCE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immCaseFile.getId() + "/immigration/work-right")
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("titreType", "VLS_TS_SALARIE", "country", "FRANCE"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_afterPost_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immCaseFile.getId() + "/immigration/work-right")
                        .with(authentication(authImm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("titreType", "CARTE_RESIDENT", "country", "FRANCE"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immCaseFile.getId() + "/immigration/work-right")
                        .with(authentication(authImm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titreType").value("CARTE_RESIDENT"))
                .andExpect(jsonPath("$.droitTravail").value("OUI"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immCaseFile.getId() + "/immigration/work-right")
                        .with(authentication(authImm)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replaces() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immCaseFile.getId() + "/immigration/work-right")
                        .with(authentication(authImm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("titreType", "VLS_TS_SALARIE", "country", "FRANCE"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/case-files/" + immCaseFile.getId() + "/immigration/work-right")
                        .with(authentication(authImm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("titreType", "PERMIS_UNIQUE", "country", "BELGIQUE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titreType").value("PERMIS_UNIQUE"))
                .andExpect(jsonPath("$.country").value("BELGIQUE"));
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
