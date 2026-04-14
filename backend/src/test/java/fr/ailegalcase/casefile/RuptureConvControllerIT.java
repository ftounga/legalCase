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
class RuptureConvControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired RuptureConvAnalysisRepository analysisRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authTravail;
    private OAuth2AuthenticationToken authOther;
    private CaseFile travailCaseFile;
    private CaseFile immCaseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User u1 = new User(); u1.setEmail("rc-" + ts + "@example.com"); u1.setStatus("ACTIVE");
        u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-rc-" + ts);
        authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("RC " + ts); ws1.setSlug("ws-rc-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_DU_TRAVAIL"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE");
        ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true);
        workspaceMemberRepository.save(m1);
        travailCaseFile = new CaseFile(); travailCaseFile.setTitle("RC " + ts); travailCaseFile.setWorkspace(ws1);
        travailCaseFile.setCreatedBy(u1); travailCaseFile.setLegalDomain("DROIT_DU_TRAVAIL"); travailCaseFile.setStatus("OPEN");
        travailCaseFile = caseFileRepository.save(travailCaseFile);
        authTravail = buildAuth("g-rc-" + ts, "rc-" + ts + "@example.com");

        User u2 = new User(); u2.setEmail("rc-o-" + ts + "@example.com"); u2.setStatus("ACTIVE");
        u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-rc-o-" + ts);
        authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("Imm " + ts); ws2.setSlug("ws-rc-o-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_IMMIGRATION"); ws2.setCountry("FRANCE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE");
        ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true);
        workspaceMemberRepository.save(m2);
        immCaseFile = new CaseFile(); immCaseFile.setTitle("Imm " + ts); immCaseFile.setWorkspace(ws2);
        immCaseFile.setCreatedBy(u2); immCaseFile.setLegalDomain("DROIT_IMMIGRATION"); immCaseFile.setStatus("OPEN");
        immCaseFile = caseFileRepository.save(immCaseFile);
        authOther = buildAuth("g-rc-o-" + ts, "rc-o-" + ts + "@example.com");
    }

    @Test
    void POST_france_allOui_returns200_valide() throws Exception {
        Map<String, Object> body = Map.of("country", "FRANCE", "reponses", Map.of(
                "RC_CONSENTEMENT", "OUI", "RC_DELAI_RETRACTATION", "OUI", "RC_HOMOLOGATION", "OUI",
                "RC_ASSISTANCE", "OUI", "RC_INDEMNITE", "OUI", "RC_ENTRETIENS", "OUI"));
        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/rupture-conv")
                        .with(authentication(authTravail)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("VALIDE"))
                .andExpect(jsonPath("$.scoreRisque").value(0))
                .andExpect(jsonPath("$.criteres.length()").value(6));
    }

    @Test
    void POST_belgique_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/rupture-conv")
                        .with(authentication(authTravail)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("country", "BELGIQUE", "reponses", Map.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_countryNull_returns400() throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("country", null);
        body.put("reponses", Map.of());
        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/rupture-conv")
                        .with(authentication(authTravail)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_wrongDomain_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immCaseFile.getId() + "/rupture-conv")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("country", "FRANCE", "reponses", Map.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/rupture-conv")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("country", "FRANCE", "reponses", Map.of()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_afterPost_returns200_coherent() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/rupture-conv")
                        .with(authentication(authTravail)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("country", "FRANCE",
                                "reponses", Map.of("RC_CONSENTEMENT", "NON")))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/case-files/" + travailCaseFile.getId() + "/rupture-conv")
                        .with(authentication(authTravail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INVALIDE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailCaseFile.getId() + "/rupture-conv")
                        .with(authentication(authTravail)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_uneSeuleLigne() throws Exception {
        String url = "/api/v1/case-files/" + travailCaseFile.getId() + "/rupture-conv";
        mockMvc.perform(post(url).with(authentication(authTravail)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("country", "FRANCE", "reponses", Map.of()))))
                .andExpect(status().isOk());
        mockMvc.perform(post(url).with(authentication(authTravail)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("country", "FRANCE",
                                "reponses", Map.of("RC_CONSENTEMENT", "OUI")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RISQUE_MODERE"));
        // Seule une ligne persistée
        org.assertj.core.api.Assertions.assertThat(
                analysisRepository.findByCaseFileId(travailCaseFile.getId())).isPresent();
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
