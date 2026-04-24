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
import java.util.LinkedHashMap;
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
class InaptitudeControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFr;
    private OAuth2AuthenticationToken authBe;
    private OAuth2AuthenticationToken authOther;
    private CaseFile travailFrCf;
    private CaseFile travailBeCf;
    private CaseFile immCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace FR — droit du travail
        User u1 = new User(); u1.setEmail("inapt-fr-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-inapt-fr-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("Inapt FR " + ts); ws1.setSlug("ws-inapt-fr-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_DU_TRAVAIL"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        travailFrCf = new CaseFile(); travailFrCf.setTitle("Inapt FR " + ts); travailFrCf.setWorkspace(ws1); travailFrCf.setCreatedBy(u1);
        travailFrCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailFrCf.setStatus("OPEN"); travailFrCf = caseFileRepository.save(travailFrCf);
        authFr = buildAuth("g-inapt-fr-" + ts, "inapt-fr-" + ts + "@ex.com");

        // Workspace BE — droit du travail
        User u2 = new User(); u2.setEmail("inapt-be-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-inapt-be-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("Inapt BE " + ts); ws2.setSlug("ws-inapt-be-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_DU_TRAVAIL"); ws2.setCountry("BELGIQUE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        travailBeCf = new CaseFile(); travailBeCf.setTitle("Inapt BE " + ts); travailBeCf.setWorkspace(ws2); travailBeCf.setCreatedBy(u2);
        travailBeCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailBeCf.setStatus("OPEN"); travailBeCf = caseFileRepository.save(travailBeCf);
        authBe = buildAuth("g-inapt-be-" + ts, "inapt-be-" + ts + "@ex.com");

        // Workspace FR — immigration (gate domaine)
        User u3 = new User(); u3.setEmail("inapt-o-" + ts + "@ex.com"); u3.setStatus("ACTIVE"); u3 = userRepository.save(u3);
        AuthAccount a3 = new AuthAccount(); a3.setUser(u3); a3.setProvider("GOOGLE"); a3.setProviderUserId("g-inapt-o-" + ts); authAccountRepository.save(a3);
        Workspace ws3 = new Workspace(); ws3.setName("InaptO " + ts); ws3.setSlug("ws-inapt-o-" + ts); ws3.setOwner(u3);
        ws3.setLegalDomain("DROIT_IMMIGRATION"); ws3.setCountry("FRANCE"); ws3.setPlanCode("STARTER"); ws3.setStatus("ACTIVE"); ws3 = workspaceRepository.save(ws3);
        WorkspaceMember m3 = new WorkspaceMember(); m3.setWorkspace(ws3); m3.setUser(u3); m3.setMemberRole("OWNER"); m3.setPrimary(true); workspaceMemberRepository.save(m3);
        immCf = new CaseFile(); immCf.setTitle("InaptO " + ts); immCf.setWorkspace(ws3); immCf.setCreatedBy(u3);
        immCf.setLegalDomain("DROIT_IMMIGRATION"); immCf.setStatus("OPEN"); immCf = caseFileRepository.save(immCf);
        authOther = buildAuth("g-inapt-o-" + ts, "inapt-o-" + ts + "@ex.com");
    }

    @Test
    void POST_nominalFRPro_returns200_andCorrectAmounts() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/inaptitude")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(2500, 8, "PROFESSIONNELLE", true, "2026-03-15"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indemniteLegale").value(10000.00))
                .andExpect(jsonPath("$.indemniteCompensatricePreavis").value(5000.00))
                .andExpect(jsonPath("$.damagesReclassement").value(0.00))
                .andExpect(jsonPath("$.total").value(15000.00))
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.origineInaptitude").value("PROFESSIONNELLE"))
                .andExpect(jsonPath("$.avisMedecinTravailDate").value("2026-03-15"))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("L.1226-10")));
    }

    @Test
    void POST_nominalFRNonPro_returns200_noPreavisNoDamages() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/inaptitude")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(2500, 8, "NON_PROFESSIONNELLE", true, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indemniteLegale").value(5000.00))
                .andExpect(jsonPath("$.indemniteCompensatricePreavis").value(0.00))
                .andExpect(jsonPath("$.damagesReclassement").value(0.00))
                .andExpect(jsonPath("$.total").value(5000.00))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("L.1226-2")));
    }

    @Test
    void POST_FRPro_reclassementNonRespecte_addsDamages12Months() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/inaptitude")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(2500, 5, "PROFESSIONNELLE", false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.damagesReclassement").value(30000.00))
                .andExpect(jsonPath("$.total").value(41250.00));
    }

    @Test
    void POST_nominalBEPro_reclassementRespecte_returnsZero() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/inaptitude")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(3000, 10, "PROFESSIONNELLE_BE", true, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indemniteLegale").value(0.00))
                .andExpect(jsonPath("$.indemniteCompensatricePreavis").value(0.00))
                .andExpect(jsonPath("$.damagesReclassement").value(0.00))
                .andExpect(jsonPath("$.total").value(0.00))
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("Art. 34 Loi 03/07/1978")));
    }

    @Test
    void POST_BENonPro_reclassementNonRespecte_returnsDamages3Months() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/inaptitude")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(3000, 5, "NON_PROFESSIONNELLE_BE", false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.damagesReclassement").value(9000.00))
                .andExpect(jsonPath("$.total").value(9000.00));
    }

    @Test
    void POST_origineFR_onWorkspaceBE_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/inaptitude")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(2500, 5, "PROFESSIONNELLE", true, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_origineBE_onWorkspaceFR_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/inaptitude")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(2500, 5, "PROFESSIONNELLE_BE", true, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salaireZero_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/inaptitude")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(0, 5, "PROFESSIONNELLE", true, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_origineAbsente_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("salaireMensuelReference", 2500);
        body.put("ancienneteAnnees", 5);
        body.put("reclassementRespecte", true);
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/inaptitude")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_immigrationCaseFile_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immCf.getId() + "/inaptitude")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(2500, 5, "PROFESSIONNELLE", true, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/inaptitude")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(2500, 5, "PROFESSIONNELLE", true, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/inaptitude")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(2500, 8, "PROFESSIONNELLE", true, null))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.indemniteLegale").value(10000.00));
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/inaptitude")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(2500, 8, "NON_PROFESSIONNELLE", true, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indemniteLegale").value(5000.00))
                .andExpect(jsonPath("$.origineInaptitude").value("NON_PROFESSIONNELLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/inaptitude")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(2500, 8, "PROFESSIONNELLE", true, "2026-03-15"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + "/inaptitude")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salaireMensuelReference").value(2500.00))
                .andExpect(jsonPath("$.ancienneteAnnees").value(8))
                .andExpect(jsonPath("$.indemniteLegale").value(10000.00))
                .andExpect(jsonPath("$.origineInaptitude").value("PROFESSIONNELLE"))
                .andExpect(jsonPath("$.reclassementRespecte").value(true))
                .andExpect(jsonPath("$.avisMedecinTravailDate").value("2026-03-15"))
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + "/inaptitude")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    private Map<String, Object> body(Object salaire, Integer anc, String origine,
                                     Boolean reclassement, String avisDate) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("salaireMensuelReference", salaire);
        m.put("ancienneteAnnees", anc);
        m.put("origineInaptitude", origine);
        m.put("reclassementRespecte", reclassement);
        if (avisDate != null) {
            m.put("avisMedecinTravailDate", avisDate);
        }
        return m;
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
