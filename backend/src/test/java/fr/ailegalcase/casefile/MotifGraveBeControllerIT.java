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
class MotifGraveBeControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authBe;
    private OAuth2AuthenticationToken authFr;
    private OAuth2AuthenticationToken authOther;
    private CaseFile travailBeCf;
    private CaseFile travailFrCf;
    private CaseFile immBeCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // BE workspace DROIT_DU_TRAVAIL
        User uBe = save(new User(), u -> { u.setEmail("mgb-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-mgb-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-mgb-be-" + ts, "mgb-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (pour vérifier le reject)
        User uFr = save(new User(), u -> { u.setEmail("mgb-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-mgb-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-mgb-fr-" + ts, "mgb-fr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (pour vérifier reject legal_domain)
        User uOt = save(new User(), u -> { u.setEmail("mgb-o-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-mgb-o-" + ts);
        Workspace wsOt = saveWs(uOt, "WSOT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "COT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-mgb-o-" + ts, "mgb-o-" + ts + "@ex.com");
    }

    @Test
    void POST_be_nominal_valid_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/motif-grave-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dateConnaissanceFait", "2026-03-02",
                                "dateNotificationRupture", "2026-03-04",
                                "dateNotificationMotifs", "2026-03-06",
                                "anciennetteAnnees", 5,
                                "salaireMensuelReference", 3500.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifGraveProceduralementValide").value(true))
                .andExpect(jsonPath("$.delaiRuptureJoursOuvrables").value(2))
                .andExpect(jsonPath("$.delaiMotifsJoursOuvrables").value(2))
                .andExpect(jsonPath("$.indemnitePreavisSiInvalide").value(0.00))
                .andExpect(jsonPath("$.baseJuridique").value(org.hamcrest.Matchers.containsString("Art. 35")));
    }

    @Test
    void POST_be_nominal_invalid_returns200WithIndemnite() throws Exception {
        // Rupture 4j ouvrables → invalide
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/motif-grave-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dateConnaissanceFait", "2026-03-02",
                                "dateNotificationRupture", "2026-03-06",
                                "dateNotificationMotifs", "2026-03-11",
                                "anciennetteAnnees", 5,
                                "salaireMensuelReference", 3500.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifGraveProceduralementValide").value(false))
                .andExpect(jsonPath("$.delaiRuptureJoursOuvrables").value(4))
                .andExpect(jsonPath("$.indemnitePreavisSiInvalide")
                        .value(org.hamcrest.Matchers.greaterThan(12000.0)));
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/motif-grave-be")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dateConnaissanceFait", "2026-03-02",
                                "dateNotificationRupture", "2026-03-04",
                                "dateNotificationMotifs", "2026-03-06",
                                "anciennetteAnnees", 5,
                                "salaireMensuelReference", 3500.00))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_immigrationCaseFile_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/motif-grave-be")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dateConnaissanceFait", "2026-03-02",
                                "dateNotificationRupture", "2026-03-04",
                                "dateNotificationMotifs", "2026-03-06",
                                "anciennetteAnnees", 5,
                                "salaireMensuelReference", 3500.00))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authBe tente d'accéder au dossier du workspace FR → 404 (isolation)
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/motif-grave-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dateConnaissanceFait", "2026-03-02",
                                "dateNotificationRupture", "2026-03-04",
                                "dateNotificationMotifs", "2026-03-06",
                                "anciennetteAnnees", 5,
                                "salaireMensuelReference", 3500.00))))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_datesIncoherentes_returns400() throws Exception {
        // Rupture avant connaissance
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/motif-grave-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dateConnaissanceFait", "2026-03-05",
                                "dateNotificationRupture", "2026-03-02",
                                "dateNotificationMotifs", "2026-03-06",
                                "anciennetteAnnees", 5,
                                "salaireMensuelReference", 3500.00))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salaireZero_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/motif-grave-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dateConnaissanceFait", "2026-03-02",
                                "dateNotificationRupture", "2026-03-04",
                                "dateNotificationMotifs", "2026-03-06",
                                "anciennetteAnnees", 5,
                                "salaireMensuelReference", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/motif-grave-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dateConnaissanceFait", "2026-03-02",
                                "dateNotificationRupture", "2026-03-04",
                                "dateNotificationMotifs", "2026-03-06",
                                "anciennetteAnnees", 5,
                                "salaireMensuelReference", 3500.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anciennetteAnnees").value(5));

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/motif-grave-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dateConnaissanceFait", "2026-03-02",
                                "dateNotificationRupture", "2026-03-06",
                                "dateNotificationMotifs", "2026-03-11",
                                "anciennetteAnnees", 10,
                                "salaireMensuelReference", 4000.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anciennetteAnnees").value(10))
                .andExpect(jsonPath("$.motifGraveProceduralementValide").value(false));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/motif-grave-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dateConnaissanceFait", "2026-03-02",
                                "dateNotificationRupture", "2026-03-04",
                                "dateNotificationMotifs", "2026-03-06",
                                "anciennetteAnnees", 5,
                                "salaireMensuelReference", 3500.00))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + "/motif-grave-be")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifGraveProceduralementValide").value(true))
                .andExpect(jsonPath("$.delaiRuptureJoursOuvrables").value(2));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + "/motif-grave-be")
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    // ---- helpers ----

    private User save(User u, java.util.function.Consumer<User> init) {
        init.accept(u);
        return userRepository.save(u);
    }

    private void saveAuth(User user, String providerUserId) {
        AuthAccount a = new AuthAccount();
        a.setUser(user); a.setProvider("GOOGLE"); a.setProviderUserId(providerUserId);
        authAccountRepository.save(a);
    }

    private Workspace saveWs(User owner, String name, String legalDomain, String country) {
        Workspace ws = new Workspace();
        ws.setName(name); ws.setSlug(name.toLowerCase().replace(' ', '-'));
        ws.setOwner(owner); ws.setLegalDomain(legalDomain); ws.setCountry(country);
        ws.setPlanCode("STARTER"); ws.setStatus("ACTIVE");
        return workspaceRepository.save(ws);
    }

    private void saveMember(User user, Workspace ws) {
        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(ws); m.setUser(user); m.setMemberRole("OWNER"); m.setPrimary(true);
        workspaceMemberRepository.save(m);
    }

    private CaseFile saveCf(User user, Workspace ws, String title, String domain) {
        CaseFile cf = new CaseFile();
        cf.setTitle(title); cf.setWorkspace(ws); cf.setCreatedBy(user);
        cf.setLegalDomain(domain); cf.setStatus("OPEN");
        return caseFileRepository.save(cf);
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
