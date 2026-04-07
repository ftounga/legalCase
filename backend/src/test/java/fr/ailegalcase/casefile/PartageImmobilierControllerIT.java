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
class PartageImmobilierControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFamille;
    private OAuth2AuthenticationToken authOther;
    private CaseFile familleCf;
    private CaseFile travailCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User u1 = new User(); u1.setEmail("pi-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-pi-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("PI " + ts); ws1.setSlug("ws-pi-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_FAMILLE"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        familleCf = new CaseFile(); familleCf.setTitle("PI " + ts); familleCf.setWorkspace(ws1); familleCf.setCreatedBy(u1);
        familleCf.setLegalDomain("DROIT_FAMILLE"); familleCf.setStatus("OPEN"); familleCf = caseFileRepository.save(familleCf);
        authFamille = buildAuth("g-pi-" + ts, "pi-" + ts + "@ex.com");

        User u2 = new User(); u2.setEmail("pi-o-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-pi-o-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("PO " + ts); ws2.setSlug("ws-pi-o-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_DU_TRAVAIL"); ws2.setCountry("FRANCE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        travailCf = new CaseFile(); travailCf.setTitle("PO " + ts); travailCf.setWorkspace(ws2); travailCf.setCreatedBy(u2);
        travailCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailCf.setStatus("OPEN"); travailCf = caseFileRepository.save(travailCf);
        authOther = buildAuth("g-pi-o-" + ts, "pi-o-" + ts + "@ex.com");
    }

    @Test
    void POST_france_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + familleCf.getId() + "/partage-immobilier")
                        .with(authentication(authFamille)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("country", "FRANCE", "valeurVenale", 300000, "capitalRestantDu", 100000, "quotePartAttributaire", 0.5, "isDivorce", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soulte").isNumber())
                .andExpect(jsonPath("$.droitPartage").isNumber());
    }

    @Test
    void POST_belgique_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + familleCf.getId() + "/partage-immobilier")
                        .with(authentication(authFamille)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("country", "BELGIQUE", "valeurVenale", 350000, "capitalRestantDu", 80000, "quotePartAttributaire", 0.5, "isDivorce", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"));
    }

    @Test
    void POST_invalidCountry_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + familleCf.getId() + "/partage-immobilier")
                        .with(authentication(authFamille)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("country", "ALLEMAGNE", "valeurVenale", 300000, "capitalRestantDu", 0, "quotePartAttributaire", 0.5, "isDivorce", true))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_wrongDomain_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/partage-immobilier")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("country", "FRANCE", "valeurVenale", 300000, "capitalRestantDu", 0, "quotePartAttributaire", 0.5, "isDivorce", true))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + familleCf.getId() + "/partage-immobilier")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("country", "FRANCE", "valeurVenale", 300000, "capitalRestantDu", 0, "quotePartAttributaire", 0.5, "isDivorce", true))))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_afterPost_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + familleCf.getId() + "/partage-immobilier")
                        .with(authentication(authFamille)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("country", "FRANCE", "valeurVenale", 250000, "capitalRestantDu", 50000, "quotePartAttributaire", 0.6, "isDivorce", true))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/case-files/" + familleCf.getId() + "/partage-immobilier")
                        .with(authentication(authFamille)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + familleCf.getId() + "/partage-immobilier")
                        .with(authentication(authFamille)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replaces() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + familleCf.getId() + "/partage-immobilier")
                        .with(authentication(authFamille)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("country", "FRANCE", "valeurVenale", 300000, "capitalRestantDu", 0, "quotePartAttributaire", 0.5, "isDivorce", true))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/case-files/" + familleCf.getId() + "/partage-immobilier")
                        .with(authentication(authFamille)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("country", "BELGIQUE", "valeurVenale", 400000, "capitalRestantDu", 100000, "quotePartAttributaire", 0.7, "isDivorce", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"));
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
