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
 * SF-221-03 — IT résident longue durée UE BE (art. 15bis Loi 15/12/1980 —
 * directive 2003/109/CE). Couvre 200 nominal + gates 400 FR / domaine /
 * date future + 404 isolation + GET persistant.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ResidenceLongueDureeUeBeControllerIT {

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

        User uBe = save(new User(), u -> { u.setEmail("rl-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-rl-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEIRL " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEIRL " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-rl-be-" + ts, "rl-be-" + ts + "@ex.com");

        User uFr = save(new User(), u -> { u.setEmail("rl-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-rl-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRIRL " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRIRL " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-rl-fr-" + ts, "rl-fr-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("rl-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-rl-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSBETRL " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDt, wsDt);
        dtBeCf = saveCf(uDt, wsDt, "CBETRL " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-rl-dt-" + ts, "rl-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String dateDebut, boolean ininterrompu,
                                     boolean ressources, boolean assurance,
                                     boolean integration, boolean absencesExcessives) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateDebutSejourLegal", dateDebut);
        m.put("sejourLegalIninterrompu", ininterrompu);
        m.put("ressourcesStablesSuffisantes", ressources);
        m.put("assuranceMaladie", assurance);
        m.put("conditionIntegrationRemplie", integration);
        m.put("absencesHorsUeExcessives", absencesExcessives);
        return m;
    }

    @Test
    void POST_be_nominal_eligible_returns200() throws Exception {
        String debut = LocalDate.now().minusYears(6).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/residence-longue-duree-ue-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, true, true, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.moisRestants").value(0))
                .andExpect(jsonPath("$.basesJuridiques[0]")
                        .value(org.hamcrest.Matchers.containsString("art. 15bis")));
    }

    @Test
    void POST_be_conditionsMaterielles_returns200() throws Exception {
        String debut = LocalDate.now().minusYears(6).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/residence-longue-duree-ue-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, false, true, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONDITIONS_MATERIELLES_NON_REUNIES"))
                .andExpect(jsonPath("$.conditionsManquantes", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void POST_be_dureeInsuffisante_returns200() throws Exception {
        String debut = LocalDate.now().minusMonths(36).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/residence-longue-duree-ue-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, true, true, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DUREE_INSUFFISANTE"))
                .andExpect(jsonPath("$.moisRestants").value(24));
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        String debut = LocalDate.now().minusYears(6).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/residence-longue-duree-ue-be-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, true, true, true, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        String debut = LocalDate.now().minusYears(6).toString();
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/residence-longue-duree-ue-be-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, true, true, true, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateFuture_returns400() throws Exception {
        String debut = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/residence-longue-duree-ue-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, true, true, true, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        String debut = LocalDate.now().minusYears(6).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/residence-longue-duree-ue-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, true, true, true, false))))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        String debut = LocalDate.now().minusYears(6).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/residence-longue-duree-ue-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(debut, true, true, true, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"));

        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/residence-longue-duree-ue-be-analysis")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.assuranceMaladie").value(true));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/residence-longue-duree-ue-be-analysis")
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
