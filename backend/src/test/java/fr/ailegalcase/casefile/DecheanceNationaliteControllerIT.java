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
 * SF-220-05 : tests d'intégration de {@link DecheanceNationaliteController}.
 * Couvre POST/GET, gates country + domaine, validation enum, upsert, 404 et isolation workspace.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class DecheanceNationaliteControllerIT {

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

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User uFr = save(new User(), u -> { u.setEmail("dech-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-dech-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI-DECH " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI-DECH " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-dech-fr-" + ts, "dech-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("dech-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-dech-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI-DECH " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI-DECH " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-dech-be-" + ts, "dech-be-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("dech-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-dech-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT-DECH " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT-DECH " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-dech-dt-" + ts, "dech-dt-" + ts + "@ex.com");
    }

    // ── Helpers de corps de requête ─────────────────────────────────────

    /** Binational, faits dans le délai, motif terrorisme → CONDITIONS_REUNIES. */
    private Map<String, Object> bodyConditionsReunies() {
        Map<String, Object> m = new HashMap<>();
        m.put("motif", "TERRORISME");
        m.put("binational", true);
        m.put("dateAcquisitionNationalite", "2010-01-01");
        m.put("dateFaits", "2015-06-01");
        m.put("mesurePrononcee", false);
        return m;
    }

    /** Non binational → MESURE_IRREGULIERE (apatridie). */
    private Map<String, Object> bodyApatridie() {
        Map<String, Object> m = new HashMap<>();
        m.put("motif", "TERRORISME");
        m.put("binational", false);
        m.put("dateAcquisitionNationalite", "2010-01-01");
        m.put("dateFaits", "2012-01-01");
        m.put("mesurePrononcee", false);
        return m;
    }

    /** Mesure prononcée récemment → délai de recours calculé. */
    private Map<String, Object> bodyMesurePrononcee() {
        Map<String, Object> m = new HashMap<>();
        m.put("motif", "TERRORISME");
        m.put("binational", true);
        m.put("dateAcquisitionNationalite", "2010-01-01");
        m.put("dateFaits", "2015-06-01");
        m.put("mesurePrononcee", true);
        m.put("dateDecret", LocalDate.now().minusDays(10).toString());
        return m;
    }

    // ── Tests nominaux ──────────────────────────────────────────────────

    @Test
    void POST_fr_conditionsReunies() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConditionsReunies())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.validite").value("CONDITIONS_REUNIES"))
                .andExpect(jsonPath("$.basesJuridiques").isNotEmpty());
    }

    @Test
    void POST_fr_apatridie_mesureIrreguliere() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyApatridie())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validite").value("MESURE_IRREGULIERE"))
                .andExpect(jsonPath("$.conditionsManquantes").isNotEmpty());
    }

    @Test
    void POST_fr_mesurePrononcee_delaiRecoursEtVoieConseilEtat() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyMesurePrononcee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delaiRecoursJours").value(60))
                .andExpect(jsonPath("$.voiesRecours").isNotEmpty());
    }

    // ── Gates ───────────────────────────────────────────────────────────

    @Test
    void POST_workspaceBelgique_returns400() throws Exception {
        mockMvc.perform(post(url(immBeCf)).with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConditionsReunies())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConditionsReunies())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_motifInvalide_returns400() throws Exception {
        Map<String, Object> m = bodyConditionsReunies();
        m.put("motif", "ESPIONNAGE");
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(m)))
                .andExpect(status().isBadRequest());
    }

    // ── Isolation workspace ─────────────────────────────────────────────

    @Test
    void POST_autreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(url(immBeCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConditionsReunies())))
                .andExpect(status().isNotFound());
    }

    // ── Upsert + GET ────────────────────────────────────────────────────

    @Test
    void POST_deuxFois_upsert_retourneDernierResultat() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConditionsReunies())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validite").value("CONDITIONS_REUNIES"));

        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyApatridie())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validite").value("MESURE_IRREGULIERE"));
    }

    @Test
    void GET_afterPost_retourneAnalysePersistee() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConditionsReunies())))
                .andExpect(status().isOk());

        mockMvc.perform(get(url(immFrCf)).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.validite").value("CONDITIONS_REUNIES"))
                .andExpect(jsonPath("$.binational").value(true))
                .andExpect(jsonPath("$.motif").value("TERRORISME"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(url(immFrCf)).with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private String url(CaseFile cf) {
        return "/api/v1/case-files/" + cf.getId() + "/decheance-nationalite-analysis";
    }

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
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
