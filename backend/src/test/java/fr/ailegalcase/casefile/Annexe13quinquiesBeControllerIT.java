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
 * SF-215-17 — IT Annexe 13quinquies OQT + interdiction d'entrée art. 74/11 BE
 * (BELGIQUE UNIQUEMENT — droit des étrangers).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class Annexe13quinquiesBeControllerIT {

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

        User uBe = save(new User(), u -> { u.setEmail("a13q-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-a13q-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEIA13Q " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEIA13Q " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-a13q-be-" + ts, "a13q-be-" + ts + "@ex.com");

        User uFr = save(new User(), u -> { u.setEmail("a13q-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-a13q-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRIA13Q " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRIA13Q " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-a13q-fr-" + ts, "a13q-fr-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("a13q-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-a13q-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSBETA13Q " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDt, wsDt);
        dtBeCf = saveCf(uDt, wsDt, "CBETA13Q " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-a13q-dt-" + ts, "a13q-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String dateNotif, String motif,
                                     boolean precedentSejour, boolean recoursForme, String dateRecours) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateNotificationAnnexe", dateNotif);
        m.put("motifInterdictionEntree", motif);
        m.put("precedentSejour", precedentSejour);
        m.put("recoursForme", recoursForme);
        if (dateRecours != null) {
            m.put("dateRecours", dateRecours);
        }
        return m;
    }

    @Test
    void POST_be_nominal_sejourIrregulier3ans_returns200() throws Exception {
        String notif = LocalDate.now().toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/annexe13quinquies-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notif, "SEJOUR_IRREGULIER", false, false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifInterdictionEntree").value("SEJOUR_IRREGULIER"))
                .andExpect(jsonPath("$.dureeInterdiction").value(3))
                .andExpect(jsonPath("$.statutRecours").value("DISPONIBLE"))
                .andExpect(jsonPath("$.joursRestantsRecours").value(30))
                .andExpect(jsonPath("$.conditionsLevee").isArray())
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("74/11")));
    }

    @Test
    void POST_be_decisionJudiciaire_duree8ans_returns200() throws Exception {
        String notif = LocalDate.now().toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/annexe13quinquies-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notif, "DECISION_JUDICIAIRE", false, false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureeInterdiction").value(8));
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        String notif = LocalDate.now().toString();
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/annexe13quinquies-be-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notif, "SEJOUR_IRREGULIER", false, false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        String notif = LocalDate.now().toString();
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/annexe13quinquies-be-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notif, "SEJOUR_IRREGULIER", false, false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateNotificationFuture_returns400() throws Exception {
        String notifFuture = LocalDate.now().plusDays(5).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/annexe13quinquies-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifFuture, "SEJOUR_IRREGULIER", false, false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_recoursFormeSansDateRecours_returns400() throws Exception {
        String notif = LocalDate.now().toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/annexe13quinquies-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notif, "SEJOUR_IRREGULIER", false, true, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        String notif = LocalDate.now().toString();
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/annexe13quinquies-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notif, "SEJOUR_IRREGULIER", false, false, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        String notif = LocalDate.now().minusDays(5).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/annexe13quinquies-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notif, "SEJOUR_IRREGULIER", true, true,
                                        LocalDate.now().minusDays(2).toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureeInterdiction").value(5))
                .andExpect(jsonPath("$.statutRecours").value("FORME"));

        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/annexe13quinquies-be-analysis")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifInterdictionEntree").value("SEJOUR_IRREGULIER"))
                .andExpect(jsonPath("$.statutRecours").value("FORME"))
                .andExpect(jsonPath("$.recoursForme").value(true));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/annexe13quinquies-be-analysis")
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
