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
class LicenciementNulDetectionControllerIT {

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
        User u1 = new User(); u1.setEmail("lnd-fr-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-lnd-fr-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("LND FR " + ts); ws1.setSlug("ws-lnd-fr-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_DU_TRAVAIL"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        travailFrCf = new CaseFile(); travailFrCf.setTitle("LND FR " + ts); travailFrCf.setWorkspace(ws1); travailFrCf.setCreatedBy(u1);
        travailFrCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailFrCf.setStatus("OPEN"); travailFrCf = caseFileRepository.save(travailFrCf);
        authFr = buildAuth("g-lnd-fr-" + ts, "lnd-fr-" + ts + "@ex.com");

        // Workspace BE — droit du travail (doit refuser : SF FR seulement)
        User u2 = new User(); u2.setEmail("lnd-be-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-lnd-be-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("LND BE " + ts); ws2.setSlug("ws-lnd-be-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_DU_TRAVAIL"); ws2.setCountry("BELGIQUE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        travailBeCf = new CaseFile(); travailBeCf.setTitle("LND BE " + ts); travailBeCf.setWorkspace(ws2); travailBeCf.setCreatedBy(u2);
        travailBeCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailBeCf.setStatus("OPEN"); travailBeCf = caseFileRepository.save(travailBeCf);
        authBe = buildAuth("g-lnd-be-" + ts, "lnd-be-" + ts + "@ex.com");

        // Workspace FR — immigration (gate domaine)
        User u3 = new User(); u3.setEmail("lnd-o-" + ts + "@ex.com"); u3.setStatus("ACTIVE"); u3 = userRepository.save(u3);
        AuthAccount a3 = new AuthAccount(); a3.setUser(u3); a3.setProvider("GOOGLE"); a3.setProviderUserId("g-lnd-o-" + ts); authAccountRepository.save(a3);
        Workspace ws3 = new Workspace(); ws3.setName("LNDO " + ts); ws3.setSlug("ws-lnd-o-" + ts); ws3.setOwner(u3);
        ws3.setLegalDomain("DROIT_IMMIGRATION"); ws3.setCountry("FRANCE"); ws3.setPlanCode("STARTER"); ws3.setStatus("ACTIVE"); ws3 = workspaceRepository.save(ws3);
        WorkspaceMember m3 = new WorkspaceMember(); m3.setWorkspace(ws3); m3.setUser(u3); m3.setMemberRole("OWNER"); m3.setPrimary(true); workspaceMemberRepository.save(m3);
        immCf = new CaseFile(); immCf.setTitle("LNDO " + ts); immCf.setWorkspace(ws3); immCf.setCreatedBy(u3);
        immCf.setLegalDomain("DROIT_IMMIGRATION"); immCf.setStatus("OPEN"); immCf = caseFileRepository.save(immCf);
        authOther = buildAuth("g-lnd-o-" + ts, "lnd-o-" + ts + "@ex.com");
    }

    @Test
    void POST_nominalFR_oneProtection_returns200_score35Moyenne() throws Exception {
        Map<String, Object> body = body(2500, "2026-04-15");
        body.put("salarieAccidentTravail", true);
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/licenciement-nul-detection")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreNullite").value(35))
                .andExpect(jsonPath("$.verdictProbabiliteNullite").value("MOYENNE"))
                .andExpect(jsonPath("$.nombreProtectionsActives").value(1))
                .andExpect(jsonPath("$.indemniteMinimumNuliteEur").value(15000.00))
                .andExpect(jsonPath("$.indemniteMinimumMois").value(6))
                .andExpect(jsonPath("$.reintegrationOuverte").value(true))
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.protectionsDetectees[0]").value("ACCIDENT_TRAVAIL"));
    }

    @Test
    void POST_nominalFR_twoProtections_returns200_score70Elevee() throws Exception {
        Map<String, Object> body = body(2500, "2026-04-15");
        body.put("salarieAccidentTravail", true);
        body.put("salarieDiscriminationAlleguee", true);
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/licenciement-nul-detection")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreNullite").value(70))
                .andExpect(jsonPath("$.verdictProbabiliteNullite").value("ELEVEE"))
                .andExpect(jsonPath("$.nombreProtectionsActives").value(2))
                .andExpect(jsonPath("$.nulliteProbable").value(true))
                .andExpect(jsonPath("$.indemniteMinimumNuliteEur").value(15000.00))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("L.1226-9")));
    }

    @Test
    void POST_aucuneProtection_returns200_score0Faible() throws Exception {
        Map<String, Object> body = body(2500, "2026-04-15");
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/licenciement-nul-detection")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreNullite").value(0))
                .andExpect(jsonPath("$.verdictProbabiliteNullite").value("FAIBLE"))
                .andExpect(jsonPath("$.nombreProtectionsActives").value(0))
                .andExpect(jsonPath("$.nulliteProbable").value(false))
                .andExpect(jsonPath("$.reintegrationOuverte").value(false));
    }

    @Test
    void POST_workspaceBE_returns400() throws Exception {
        Map<String, Object> body = body(2500, "2026-04-15");
        body.put("salarieHarceleAvere", true);
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/licenciement-nul-detection")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dossierImmigration_returns400() throws Exception {
        Map<String, Object> body = body(2500, "2026-04-15");
        mockMvc.perform(post("/api/v1/case-files/" + immCf.getId() + "/licenciement-nul-detection")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        Map<String, Object> body = body(2500, "2026-04-15");
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/licenciement-nul-detection")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_salaireZero_returns400() throws Exception {
        Map<String, Object> body = body(0, "2026-04-15");
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/licenciement-nul-detection")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateNotificationManquante_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("salaireMensuelBrutEur", 2500);
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/licenciement-nul-detection")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsSnapshot() throws Exception {
        Map<String, Object> body = body(2500, "2026-04-15");
        body.put("salarieHarceleAvere", true);
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/licenciement-nul-detection")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + "/licenciement-nul-detection")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salaireMensuelBrutEur").value(2500.00))
                .andExpect(jsonPath("$.scoreNullite").value(35))
                .andExpect(jsonPath("$.salarieHarceleAvere").value(true))
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + "/licenciement-nul-detection")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_remplaceAnalyse() throws Exception {
        Map<String, Object> body1 = body(2000, "2026-04-15");
        body1.put("salarieHarceleAvere", true);
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/licenciement-nul-detection")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indemniteMinimumNuliteEur").value(12000.00));

        Map<String, Object> body2 = body(3000, "2026-04-15");
        body2.put("salarieHarceleAvere", true);
        body2.put("salarieDiscriminationAlleguee", true);
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/licenciement-nul-detection")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indemniteMinimumNuliteEur").value(18000.00))
                .andExpect(jsonPath("$.nombreProtectionsActives").value(2));
    }

    private Map<String, Object> body(Object salaire, String dateNotif) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dateNotificationLicenciement", dateNotif);
        m.put("salaireMensuelBrutEur", salaire);
        return m;
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
