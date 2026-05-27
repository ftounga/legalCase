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

/** SF-215-09 — IT naturalisation conjoint Belge BE (Code de la nationalité belge art. 16). */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class NaturalisationConjointBelgeBeControllerIT {

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
    private OAuth2AuthenticationToken authDt;
    private CaseFile immBeCf;
    private CaseFile immFrCf;
    private CaseFile dtBeCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User uBe = save(new User(), u -> { u.setEmail("natconj-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-natconj-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBENATCONJ " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBENATCONJ " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-natconj-be-" + ts, "natconj-be-" + ts + "@ex.com");

        User uFr = save(new User(), u -> { u.setEmail("natconj-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-natconj-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRNATCONJ " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRNATCONJ " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-natconj-fr-" + ts, "natconj-fr-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("natconj-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-natconj-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSBETNATCONJ " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDt, wsDt);
        dtBeCf = saveCf(uDt, wsDt, "CBETNATCONJ " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-natconj-dt-" + ts, "natconj-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String dateMarriage, boolean cohabitationLegale,
                                     int dureeCohabitationMois, String niveauLangue,
                                     boolean preuveIntegration,
                                     boolean menaceOrdrePublic, boolean condamnationPenale) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateMarriage", dateMarriage);
        m.put("cohabitationLegale", cohabitationLegale);
        m.put("dureeCohabitationMois", dureeCohabitationMois);
        m.put("niveauLangue", niveauLangue);
        m.put("preuveIntegration", preuveIntegration);
        m.put("menaceOrdrePublic", menaceOrdrePublic);
        m.put("condamnationPenale", condamnationPenale);
        return m;
    }

    @Test
    void POST_be_eligible_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/naturalisation-conjoint-belge-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("2019-06-15", true, 60, "A2", true, false, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.dureeManquante").value(0))
                .andExpect(jsonPath("$.criteresNonRemplis").isArray())
                .andExpect(jsonPath("$.criteresNonRemplis").isEmpty())
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("art. 16")))
                .andExpect(jsonPath("$.delaiDeclaration")
                        .value(org.hamcrest.Matchers.containsString("officier d'état civil")));
    }

    @Test
    void POST_be_dureeInsuffisante_returns200_ineligible() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/naturalisation-conjoint-belge-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("2024-01-01", true, 24, "A2", true, false, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE"))
                .andExpect(jsonPath("$.dureeManquante").value(36))
                .andExpect(jsonPath("$.criteresNonRemplis", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("Cohabitation < 5 ans"))));
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/naturalisation-conjoint-belge-be-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("2019-06-15", true, 60, "A2", true, false, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/naturalisation-conjoint-belge-be-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("2019-06-15", true, 60, "A2", true, false, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authBe tente d'accéder au dossier FR → 404 (isolation workspace)
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/naturalisation-conjoint-belge-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("2019-06-15", true, 60, "A2", true, false, false))))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dureeOutOfBound_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/naturalisation-conjoint-belge-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("2019-06-15", true, 601, "A2", true, false, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateMarriageFuture_returns400() throws Exception {
        String tomorrow = java.time.LocalDate.now().plusDays(1).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/naturalisation-conjoint-belge-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(tomorrow, true, 60, "A2", true, false, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/naturalisation-conjoint-belge-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("2019-06-15", true, 60, "A2", true, false, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"));

        // Replay avec condamnation → INELIGIBLE
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/naturalisation-conjoint-belge-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("2019-06-15", true, 72, "A2", true, false, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE"))
                .andExpect(jsonPath("$.criteresNonRemplis", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("Condamnation"))));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/naturalisation-conjoint-belge-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("2018-05-10", true, 84, "SUPERIEUR_A2", true, false, false))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/naturalisation-conjoint-belge-be-analysis")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateMarriage").value("2018-05-10"))
                .andExpect(jsonPath("$.dureeCohabitationMois").value(84))
                .andExpect(jsonPath("$.niveauLangue").value("SUPERIEUR_A2"))
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/naturalisation-conjoint-belge-be-analysis")
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
