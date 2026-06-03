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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SF-221-02 — IT carte B séjour illimité BE (art. 14 Loi 15/12/1980 —
 * passage carte A → séjour illimité après 5 ans).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class CarteBSejourIllimiteBeControllerIT {

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

        User uBe = save(new User(), u -> { u.setEmail("cb-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-cb-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEICB " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEICB " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-cb-be-" + ts, "cb-be-" + ts + "@ex.com");

        User uFr = save(new User(), u -> { u.setEmail("cb-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-cb-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRICB " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRICB " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-cb-fr-" + ts, "cb-fr-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("cb-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-cb-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSBETCB " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDt, wsDt);
        dtBeCf = saveCf(uDt, wsDt, "CBETCB " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-cb-dt-" + ts, "cb-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String dateDebut, boolean ininterrompu,
                                     boolean absencesExcessives, boolean motifStable,
                                     boolean ordrePublic) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateDebutSejourRegulier", dateDebut);
        m.put("sejourIninterrompu", ininterrompu);
        m.put("absencesSuperieuresLimites", absencesExcessives);
        m.put("motifSejourStable", motifStable);
        m.put("ordrePublicRisque", ordrePublic);
        return m;
    }

    @Test
    void POST_be_nominal_eligible_returns200() throws Exception {
        String debut = LocalDate.now().minusYears(6).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/carte-b-sejour-illimite-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, false, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.moisRestants").value(0))
                .andExpect(jsonPath("$.basesJuridiques[0]")
                        .value(org.hamcrest.Matchers.containsString("art. 14")));
    }

    @Test
    void POST_be_dureeInsuffisante_returns200() throws Exception {
        String debut = LocalDate.now().minusMonths(36).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/carte-b-sejour-illimite-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, false, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DUREE_INSUFFISANTE"))
                .andExpect(jsonPath("$.moisRestants").value(24));
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        String debut = LocalDate.now().minusYears(6).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/carte-b-sejour-illimite-be-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, false, true, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        String debut = LocalDate.now().minusYears(6).toString();
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/carte-b-sejour-illimite-be-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, false, true, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateFuture_returns400() throws Exception {
        String debut = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/carte-b-sejour-illimite-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, false, true, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        String debut = LocalDate.now().minusYears(6).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/carte-b-sejour-illimite-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, false, true, false))))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        String debut = LocalDate.now().minusYears(6).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/carte-b-sejour-illimite-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, false, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"));

        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/carte-b-sejour-illimite-be-analysis")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.motifSejourStable").value(true));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/carte-b-sejour-illimite-be-analysis")
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
