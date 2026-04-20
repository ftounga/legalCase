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
import java.util.HashMap;
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
class RuptureConvIndemniteControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken auth;
    private OAuth2AuthenticationToken authOther;
    private CaseFile travailCf;
    private CaseFile immCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User u1 = new User(); u1.setEmail("rci-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-rci-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("RCI " + ts); ws1.setSlug("ws-rci-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_DU_TRAVAIL"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        travailCf = new CaseFile(); travailCf.setTitle("RCI " + ts); travailCf.setWorkspace(ws1); travailCf.setCreatedBy(u1);
        travailCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailCf.setStatus("OPEN"); travailCf = caseFileRepository.save(travailCf);
        auth = buildAuth("g-rci-" + ts, "rci-" + ts + "@ex.com");

        User u2 = new User(); u2.setEmail("rci-o-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-rci-o-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("IO " + ts); ws2.setSlug("ws-rci-o-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_IMMIGRATION"); ws2.setCountry("FRANCE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        immCf = new CaseFile(); immCf.setTitle("IO " + ts); immCf.setWorkspace(ws2); immCf.setCreatedBy(u2);
        immCf.setLegalDomain("DROIT_IMMIGRATION"); immCf.setStatus("OPEN"); immCf = caseFileRepository.save(immCf);
        authOther = buildAuth("g-rci-o-" + ts, "rci-o-" + ts + "@ex.com");
    }

    @Test
    void POST_valide_returns200_avecIndemniteCalculee() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/rupture-conv-indemnite")
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ancienneteAnnees", 4, "salaireMensuel", 2979))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ancienneteAnnees").value(4))
                .andExpect(jsonPath("$.indemniteLegaleMinimum").value(2979.00))
                .andExpect(jsonPath("$.baseJuridique").value("Art. R1234-2 Code du travail"))
                .andExpect(jsonPath("$.messages").isArray());
    }

    @Test
    void POST_ancienneteNull_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("ancienneteAnnees", null);
        body.put("salaireMensuel", 3000);
        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/rupture-conv-indemnite")
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_ancienneteNegative_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/rupture-conv-indemnite")
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ancienneteAnnees", -1, "salaireMensuel", 3000))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salaireNull_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("ancienneteAnnees", 5);
        body.put("salaireMensuel", null);
        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/rupture-conv-indemnite")
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salaireZero_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/rupture-conv-indemnite")
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ancienneteAnnees", 5, "salaireMensuel", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_wrongDomain_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immCf.getId() + "/rupture-conv-indemnite")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ancienneteAnnees", 5, "salaireMensuel", 3000))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/rupture-conv-indemnite")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ancienneteAnnees", 5, "salaireMensuel", 3000))))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailCf.getId() + "/rupture-conv-indemnite")
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_afterPost_returnsPersistedAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/rupture-conv-indemnite")
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ancienneteAnnees", 15, "salaireMensuel", 3000))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/case-files/" + travailCf.getId() + "/rupture-conv-indemnite")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ancienneteAnnees").value(15))
                .andExpect(jsonPath("$.indemniteLegaleMinimum").value(12500.00));
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
