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
class RequalificationCddCdiControllerIT {

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

        User u1 = new User(); u1.setEmail("rqcdd-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-rqcdd-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("RQCDD " + ts); ws1.setSlug("ws-rqcdd-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_DU_TRAVAIL"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        travailCf = new CaseFile(); travailCf.setTitle("RQCDD " + ts); travailCf.setWorkspace(ws1); travailCf.setCreatedBy(u1);
        travailCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailCf.setStatus("OPEN"); travailCf = caseFileRepository.save(travailCf);
        auth = buildAuth("g-rqcdd-" + ts, "rqcdd-" + ts + "@ex.com");

        User u2 = new User(); u2.setEmail("rqcdd-o-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-rqcdd-o-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("RQCDDO " + ts); ws2.setSlug("ws-rqcdd-o-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_IMMIGRATION"); ws2.setCountry("FRANCE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        immCf = new CaseFile(); immCf.setTitle("RQCDDO " + ts); immCf.setWorkspace(ws2); immCf.setCreatedBy(u2);
        immCf.setLegalDomain("DROIT_IMMIGRATION"); immCf.setStatus("OPEN"); immCf = caseFileRepository.save(immCf);
        authOther = buildAuth("g-rqcdd-o-" + ts, "rqcdd-o-" + ts + "@ex.com");
    }

    private Map<String, Object> nominalRequestBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("motifCddInvoque", "ACCROISSEMENT_TEMPORAIRE");
        body.put("motifInterdit", false);
        body.put("motifInterditType", null);
        body.put("successionCdd", List.of(
                Map.of("dateDebut", "2024-01-01", "dateFin", "2024-06-30", "motif", "ACCROISSEMENT"),
                Map.of("dateDebut", "2024-08-01", "dateFin", "2024-12-31", "motif", "REMPLACEMENT")
        ));
        body.put("delaiCarenceRespecte", false);
        body.put("dureeContratMois", 12);
        body.put("salaireMensuelBrutEur", 2500.00);
        body.put("dateFinDernierContrat", "2024-12-31");
        return body;
    }

    @Test
    void POST_nominal_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/requalification-cdd-cdi")
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalRequestBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreRequalification").value(20))
                .andExpect(jsonPath("$.verdictProbabiliteRequalification").value("FAIBLE"))
                .andExpect(jsonPath("$.indemniteRequalificationEur").value(2500.00))
                .andExpect(jsonPath("$.indemnitePrecariteEur").value(3000.00))
                .andExpect(jsonPath("$.totalDommagesIndemniteEur").value(5500.00))
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    @Test
    void POST_motifInterditTrue_returnsScore50() throws Exception {
        Map<String, Object> body = nominalRequestBody();
        body.put("motifInterdit", true);
        body.put("motifInterditType", "EMPLOI_PERMANENT");

        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/requalification-cdd-cdi")
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                // 50 (motif interdit) + 0 (succession 2 < 3) + 20 (carence) + 0 = 70
                .andExpect(jsonPath("$.scoreRequalification").value(70))
                .andExpect(jsonPath("$.verdictProbabiliteRequalification").value("ELEVEE"));
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        // 1er POST
        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/requalification-cdd-cdi")
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalRequestBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreRequalification").value(20));

        // 2e POST avec motif interdit
        Map<String, Object> body2 = nominalRequestBody();
        body2.put("motifInterdit", true);
        body2.put("motifInterditType", "EMPLOI_PERMANENT");

        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/requalification-cdd-cdi")
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreRequalification").value(70));
    }

    @Test
    void POST_invalidInput_emptySuccession_returns400() throws Exception {
        Map<String, Object> body = nominalRequestBody();
        body.put("successionCdd", List.of());

        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/requalification-cdd-cdi")
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_motifInterditWithoutType_returns400() throws Exception {
        Map<String, Object> body = nominalRequestBody();
        body.put("motifInterdit", true);
        body.put("motifInterditType", null);

        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/requalification-cdd-cdi")
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_immigrationCaseFile_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immCf.getId() + "/requalification-cdd-cdi")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalRequestBody())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/requalification-cdd-cdi")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalRequestBody())))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/requalification-cdd-cdi")
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalRequestBody())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailCf.getId() + "/requalification-cdd-cdi")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreRequalification").value(20))
                .andExpect(jsonPath("$.indemniteRequalificationEur").value(2500.00))
                .andExpect(jsonPath("$.indemnitePrecariteEur").value(3000.00))
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailCf.getId() + "/requalification-cdd-cdi")
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
