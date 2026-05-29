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
 * SF-214-33 : tests d'intégration de {@link AppelCaaCassationController}.
 * Couvre POST/GET, gates country + domaine, date de jugement future, upsert, 404 et
 * isolation workspace.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class AppelCaaCassationControllerIT {

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

        User uFr = save(new User(), u -> { u.setEmail("appelcaa-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-appelcaa-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI-APPELCAA " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI-APPELCAA " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-appelcaa-fr-" + ts, "appelcaa-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("appelcaa-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-appelcaa-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI-APPELCAA " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI-APPELCAA " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-appelcaa-be-" + ts, "appelcaa-be-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("appelcaa-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-appelcaa-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT-APPELCAA " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT-APPELCAA " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-appelcaa-dt-" + ts, "appelcaa-dt-" + ts + "@ex.com");
    }

    // ── Helpers de corps de requête ─────────────────────────────────────

    /** Jugement récent, REFUS_TITRE, droit commun → APPEL_POSSIBLE. */
    private Map<String, Object> bodyAppelPossible() {
        return body(LocalDate.now().minusDays(3).toString(), "REJET", "REFUS_TITRE", false);
    }

    /** OQTF jugement aujourd'hui, délai spécial 15 j → URGENT, filtrePourvois true. */
    private Map<String, Object> bodyOqtfSpecial() {
        return body(LocalDate.now().toString(), "REJET", "OQTF", true);
    }

    private Map<String, Object> body(String dateJugement, String typeDecision,
                                     String typeContentieux, boolean delaiSpecialOQTF) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateJugementTA", dateJugement);
        m.put("typeDecisionTA", typeDecision);
        m.put("typeContentieux", typeContentieux);
        m.put("delaiSpecialOQTF", delaiSpecialOQTF);
        return m;
    }

    // ── Tests nominaux ──────────────────────────────────────────────────

    @Test
    void POST_fr_appelPossible_retourne200_avecEcheanceEtMotifs() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAppelPossible())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.statut").value("APPEL_POSSIBLE"))
                .andExpect(jsonPath("$.dateEcheanceAppelCaa").exists())
                .andExpect(jsonPath("$.delaiCassationCeMois").value(2))
                .andExpect(jsonPath("$.filtrePourvoisCassation").value(false))
                .andExpect(jsonPath("$.motifsAppelPossibles").isArray())
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("R. 811-2")));
    }

    @Test
    void POST_fr_oqtfDelaiSpecial_retourneUrgent_filtrePourvoisTrue() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyOqtfSpecial())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("URGENT"))
                .andExpect(jsonPath("$.delaiSpecialOQTF").value(true))
                .andExpect(jsonPath("$.filtrePourvoisCassation").value(true));
    }

    // ── Gates ───────────────────────────────────────────────────────────

    @Test
    void POST_workspaceBelgique_returns400() throws Exception {
        mockMvc.perform(post(url(immBeCf)).with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAppelPossible())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAppelPossible())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateJugementFuture_returns400() throws Exception {
        Map<String, Object> b = body(LocalDate.now().plusDays(3).toString(),
                "REJET", "REFUS_TITRE", false);
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    // ── Isolation workspace ─────────────────────────────────────────────

    @Test
    void POST_autreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(url(immBeCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAppelPossible())))
                .andExpect(status().isNotFound());
    }

    // ── Upsert + GET ────────────────────────────────────────────────────

    @Test
    void POST_deuxFois_upsert_retourneDernierResultat() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAppelPossible())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("APPEL_POSSIBLE"));

        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyOqtfSpecial())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("URGENT"));
    }

    @Test
    void GET_afterPost_retourneAnalysePersistee() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAppelPossible())))
                .andExpect(status().isOk());

        mockMvc.perform(get(url(immFrCf)).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.statut").value("APPEL_POSSIBLE"))
                .andExpect(jsonPath("$.typeContentieux").value("REFUS_TITRE"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(url(immFrCf)).with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private String url(CaseFile cf) {
        return "/api/v1/case-files/" + cf.getId() + "/appel-caa-cassation-analysis";
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
