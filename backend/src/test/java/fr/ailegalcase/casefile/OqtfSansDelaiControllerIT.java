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
import java.time.LocalDateTime;
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
class OqtfSansDelaiControllerIT {

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
    private OAuth2AuthenticationToken authDt;
    private CaseFile immFrCf;
    private CaseFile immBeCf;
    private CaseFile dtFrCf;

    /** 10h avant maintenant — suffisamment récent pour rester DISPONIBLE (>24h restants), pas dans le futur. */
    private final LocalDateTime notifRecente = LocalDateTime.now().minusHours(10);

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User uFr = save(new User(), u -> { u.setEmail("oqtfs-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-oqtfs-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-oqtfs-fr-" + ts, "oqtfs-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("oqtfs-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-oqtfs-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-oqtfs-be-" + ts, "oqtfs-be-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("oqtfs-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-oqtfs-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-oqtfs-dt-" + ts, "oqtfs-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(LocalDateTime notif, String motif,
                                     boolean placementCra, boolean recoursForme,
                                     LocalDateTime dateRecours) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateHeureNotificationOqtf", notif.toString());
        m.put("motifSansDelai", motif);
        m.put("placementCra", placementCra);
        m.put("recoursForme", recoursForme);
        m.put("dateHeureRecours", dateRecours != null ? dateRecours.toString() : null);
        return m;
    }

    @Test
    void POST_fr_nominal_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/oqtf-sans-delai")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, "RISQUE_FUITE", false, false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifSansDelai").value("RISQUE_FUITE"))
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.recoursForme").value(false))
                .andExpect(jsonPath("$.placementCra").value(false))
                .andExpect(jsonPath("$.statutDelaiRecours").exists())
                .andExpect(jsonPath("$.dateHeureExpirationDelaiRecours").exists())
                .andExpect(jsonPath("$.refereDisponibles[0]").value("REFERE_LIBERTE_L521_2"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("L.731-1")));
    }

    @Test
    void POST_fr_withCra_addsControleJld() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/oqtf-sans-delai")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, "RISQUE_FUITE", true, false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placementCra").value(true))
                .andExpect(jsonPath("$.refereDisponibles[0]").value("REFERE_LIBERTE_L521_2"))
                .andExpect(jsonPath("$.refereDisponibles[1]").value("CONTROLE_JLD_LIBERTE"));
    }

    @Test
    void POST_fr_recoursForme_setsAudienceAndDecision() throws Exception {
        LocalDateTime notif = LocalDateTime.now().minusHours(30);
        LocalDateTime recours = LocalDateTime.now().minusHours(5);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/oqtf-sans-delai")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notif, "TROUBLE_ORDRE_PUBLIC", false, true, recours))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutDelaiRecours").value("RECOURS_FORME"))
                .andExpect(jsonPath("$.dateHeureAudiencePrevisionnelle").exists())
                .andExpect(jsonPath("$.dateDecisionPrevisionnelle").exists());
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/oqtf-sans-delai")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, "RISQUE_FUITE", false, false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/oqtf-sans-delai")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, "RISQUE_FUITE", false, false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/oqtf-sans-delai")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, "RISQUE_FUITE", false, false, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_invalidMotif_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/oqtf-sans-delai")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, "MOTIF_BIDON", false, false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_futureNotification_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/oqtf-sans-delai")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(LocalDateTime.now().plusHours(5),
                                        "RISQUE_FUITE", false, false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_recoursFormeSansDateRecours_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/oqtf-sans-delai")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, "RISQUE_FUITE", false, true, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/oqtf-sans-delai")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, "RISQUE_FUITE", false, false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifSansDelai").value("RISQUE_FUITE"));

        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/oqtf-sans-delai")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, "TROUBLE_ORDRE_PUBLIC", true, false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifSansDelai").value("TROUBLE_ORDRE_PUBLIC"))
                .andExpect(jsonPath("$.placementCra").value(true));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/oqtf-sans-delai")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, "OQTF_PRECEDENTE_INEXECUTEE", false, false, null))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/oqtf-sans-delai")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifSansDelai").value("OQTF_PRECEDENTE_INEXECUTEE"))
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/oqtf-sans-delai")
                        .with(authentication(authFr)))
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
