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

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class Annexe13BeControllerIT {

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

    /** 10 jours avant aujourd'hui — suffisamment récent pour éviter EXPIRE. */
    private final LocalDate notifRecente = LocalDate.now().minusDays(10);

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // BE workspace DROIT_IMMIGRATION (cas nominal)
        User uBe = save(new User(), u -> { u.setEmail("annexe13-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-annexe13-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-annexe13-be-" + ts, "annexe13-be-" + ts + "@ex.com");

        // FR workspace DROIT_IMMIGRATION (gate country BELGIQUE → rejet)
        User uFr = save(new User(), u -> { u.setEmail("annexe13-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-annexe13-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-annexe13-fr-" + ts, "annexe13-fr-" + ts + "@ex.com");

        // BE workspace DROIT_DU_TRAVAIL (gate legal_domain → rejet)
        User uDt = save(new User(), u -> { u.setEmail("annexe13-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-annexe13-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSBET " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDt, wsDt);
        dtBeCf = saveCf(uDt, wsDt, "CBET " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-annexe13-dt-" + ts, "annexe13-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(LocalDate notif, int delai, String motif,
                                     boolean transfert, boolean recoursForme,
                                     LocalDate dateRecours, String typeRecours) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateNotificationAnnexe13", notif.toString());
        m.put("delaiDepartImposeJours", delai);
        m.put("motifOqt", motif);
        m.put("transfertImminent", transfert);
        m.put("recoursForme", recoursForme);
        m.put("dateRecours", dateRecours != null ? dateRecours.toString() : null);
        m.put("typeRecours", typeRecours);
        return m;
    }

    @Test
    void POST_be_nominal_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/annexe13-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, 30, "SEJOUR_IRREGULIER_ART_7",
                                        false, false, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifOqt").value("SEJOUR_IRREGULIER_ART_7"))
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.recoursForme").value(false))
                .andExpect(jsonPath("$.transfertImminent").value(false))
                .andExpect(jsonPath("$.statutRecoursAnnulation").exists())
                .andExpect(jsonPath("$.dateExpirationRecoursAnnulation").exists())
                .andExpect(jsonPath("$.dateExpirationRecoursExtremeUrgence").exists())
                .andExpect(jsonPath("$.referedDisponibles[0]").value("DEMANDE_SUSPENSION_ART_39_2"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("15/12/1980")));
    }

    @Test
    void POST_transfertImminent_addsExtremeUrgenceRefere() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/annexe13-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, 0, "SEJOUR_IRREGULIER_ART_7",
                                        true, false, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transfertImminent").value(true))
                .andExpect(jsonPath("$.referedDisponibles[0]").value("DEMANDE_SUSPENSION_ART_39_2"))
                .andExpect(jsonPath("$.referedDisponibles[1]").value("RECOURS_EXTREME_URGENCE_39_82"));
    }

    @Test
    void POST_recoursForme_setsAudienceAndDecision() throws Exception {
        LocalDate notif = LocalDate.now().minusDays(20);
        LocalDate recours = LocalDate.now().minusDays(5);
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/annexe13-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notif, 30, "REFUS_SEJOUR_APRES_DEMANDE",
                                        false, true, recours, "ANNULATION_30J"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutRecoursAnnulation").value("RECOURS_FORME"))
                .andExpect(jsonPath("$.typeRecours").value("ANNULATION_30J"))
                .andExpect(jsonPath("$.dateAudiencePrevisionnelle").exists())
                .andExpect(jsonPath("$.dateDecisionPrevisionnelle").exists());
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/annexe13-be")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, 30, "SEJOUR_IRREGULIER_ART_7",
                                        false, false, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/annexe13-be")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, 30, "SEJOUR_IRREGULIER_ART_7",
                                        false, false, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authBe tente d'accéder au dossier du workspace FR → 404 (isolation)
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/annexe13-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, 30, "SEJOUR_IRREGULIER_ART_7",
                                        false, false, null, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_invalidMotif_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/annexe13-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, 30, "MOTIF_BIDON",
                                        false, false, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_futureNotification_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/annexe13-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(LocalDate.now().plusDays(5), 30, "SEJOUR_IRREGULIER_ART_7",
                                        false, false, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_recoursFormeSansDateRecours_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/annexe13-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, 30, "SEJOUR_IRREGULIER_ART_7",
                                        false, true, null, "ANNULATION_30J"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/annexe13-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, 30, "SEJOUR_IRREGULIER_ART_7",
                                        false, false, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifOqt").value("SEJOUR_IRREGULIER_ART_7"));

        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/annexe13-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, 15, "FIN_SEJOUR_REGULIER",
                                        true, false, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifOqt").value("FIN_SEJOUR_REGULIER"))
                .andExpect(jsonPath("$.delaiDepartImposeJours").value(15))
                .andExpect(jsonPath("$.transfertImminent").value(true));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/annexe13-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(notifRecente, 30, "REFUS_SEJOUR_APRES_DEMANDE",
                                        false, false, null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/annexe13-be")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifOqt").value("REFUS_SEJOUR_APRES_DEMANDE"))
                .andExpect(jsonPath("$.country").value("BELGIQUE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/annexe13-be")
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
