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
 * SF-214-37 : tests d'intégration de {@link ItfJudiciaireController}. Couvre
 * POST/GET, gates country + domaine, infraction > 200, 404 sans POST, isolation.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ItfJudiciaireControllerIT {

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

        User uFr = save(new User(), u -> { u.setEmail("itf-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-itf-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI-ITF " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI-ITF " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-itf-fr-" + ts, "itf-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("itf-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-itf-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI-ITF " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI-ITF " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-itf-be-" + ts, "itf-be-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("itf-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-itf-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT-ITF " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT-ITF " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-itf-dt-" + ts, "itf-dt-" + ts + "@ex.com");
    }

    // ── Helpers de corps de requête ─────────────────────────────────────

    private Map<String, Object> bodyAppelPossible() {
        Map<String, Object> m = new HashMap<>();
        m.put("dateCondamnation", LocalDate.now().minusDays(3).toString());
        m.put("dureeITFAnnees", 10);
        m.put("infractionPrincipale", "Trafic de stupéfiants");
        m.put("condamnationDefinitive", false);
        return m;
    }

    private Map<String, Object> bodyRelevePossible() {
        Map<String, Object> m = new HashMap<>();
        m.put("dateCondamnation", LocalDate.now().minusYears(6).toString());
        m.put("dureeITFAnnees", 5);
        m.put("infractionPrincipale", "Récidive");
        m.put("condamnationDefinitive", true);
        return m;
    }

    // ── Tests nominaux ──────────────────────────────────────────────────

    @Test
    void POST_fr_appelPossible_retourne200_avecVoiesRecours() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAppelPossible())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.statut").value("APPEL_POSSIBLE"))
                .andExpect(jsonPath("$.voiesRecours").isArray())
                .andExpect(jsonPath("$.requisReleve").isArray())
                .andExpect(jsonPath("$.distinctionItfVsIrtf",
                        org.hamcrest.Matchers.containsString("IRTF")))
                .andExpect(jsonPath("$.dateEcheanceReleve").exists());
    }

    @Test
    void POST_fr_relevePossible_retourneStatutReleve() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyRelevePossible())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("RELEVE_POSSIBLE"));
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
    void POST_infractionTropLongue_returns400() throws Exception {
        Map<String, Object> body = bodyAppelPossible();
        body.put("infractionPrincipale", "x".repeat(201));
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
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

    // ── GET ─────────────────────────────────────────────────────────────

    @Test
    void GET_afterPost_retourneAnalysePersistee() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAppelPossible())))
                .andExpect(status().isOk());

        mockMvc.perform(get(url(immFrCf)).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.statut").value("APPEL_POSSIBLE"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(url(immFrCf)).with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private String url(CaseFile cf) {
        return "/api/v1/case-files/" + cf.getId() + "/itf-judiciaire-analysis";
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
