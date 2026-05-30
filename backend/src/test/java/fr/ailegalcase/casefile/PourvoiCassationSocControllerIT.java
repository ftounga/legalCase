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
 * SF-218-05 : tests d'intégration de {@link PourvoiCassationSocController}.
 * Couvre POST/GET, gates country (BE → 400) + domaine (DROIT_IMMIGRATION →
 * 400), casOuverture vide (400), date future (400), isolation workspace (404),
 * GET sans POST (404) et upsert.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class PourvoiCassationSocControllerIT {

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
    private OAuth2AuthenticationToken authImm;
    private CaseFile dtFrCf;
    private CaseFile dtBeCf;
    private CaseFile immFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User uFr = save(new User(), u -> { u.setEmail("pcs-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-pcs-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRT-PCS " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        dtFrCf = saveCf(uFr, wsFr, "CFRT-PCS " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-pcs-fr-" + ts, "pcs-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("pcs-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-pcs-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBET-PCS " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        dtBeCf = saveCf(uBe, wsBe, "CBET-PCS " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-pcs-be-" + ts, "pcs-be-" + ts + "@ex.com");

        User uImm = save(new User(), u -> { u.setEmail("pcs-imm-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uImm, "g-pcs-imm-" + ts);
        Workspace wsImm = saveWs(uImm, "WSFRI-PCS " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uImm, wsImm);
        immFrCf = saveCf(uImm, wsImm, "CFRI-PCS " + ts, "DROIT_IMMIGRATION");
        authImm = buildAuth("g-pcs-imm-" + ts, "pcs-imm-" + ts + "@ex.com");
    }

    // ── Helpers de corps de requête ─────────────────────────────────────

    private Map<String, Object> bodyRecommande() {
        Map<String, Object> m = new HashMap<>();
        m.put("dateNotificationArret", LocalDate.now().minusDays(10).toString());
        m.put("casOuverture", List.of("VIOLATION_LOI"));
        m.put("representationAvocatCassation", true);
        m.put("moyenSerieuxIdentifie", true);
        return m;
    }

    private Map<String, Object> bodyRisque() {
        Map<String, Object> m = new HashMap<>();
        m.put("dateNotificationArret", LocalDate.now().minusDays(10).toString());
        m.put("casOuverture", List.of("DENATURATION"));
        m.put("representationAvocatCassation", true);
        m.put("moyenSerieuxIdentifie", false);
        return m;
    }

    // ── Tests nominaux ──────────────────────────────────────────────────

    @Test
    void POST_fr_violationLoi_retourne200_pourvoiRecommande() throws Exception {
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyRecommande())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdict").value("POURVOI_RECOMMANDE"))
                .andExpect(jsonPath("$.risqueNonAdmission").value("FAIBLE"))
                .andExpect(jsonPath("$.verdictDelai").value("DELAI_OUVERT"))
                .andExpect(jsonPath("$.casOuvertureAnalyses").isArray())
                .andExpect(jsonPath("$.checklist").isArray())
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("612")));
    }

    @Test
    void POST_fr_denaturation_pourvoiRisque() throws Exception {
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyRisque())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("POURVOI_RISQUE"))
                .andExpect(jsonPath("$.risqueNonAdmission").value("MODERE"));
    }

    // ── Gates ───────────────────────────────────────────────────────────

    @Test
    void POST_workspaceBelgique_returns400() throws Exception {
        mockMvc.perform(post(url(dtBeCf)).with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyRecommande())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitImmigration_returns400() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authImm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyRecommande())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_casOuvertureVide_returns400() throws Exception {
        Map<String, Object> body = bodyRecommande();
        body.put("casOuverture", List.of());
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateFuture_returns400() throws Exception {
        Map<String, Object> body = bodyRecommande();
        body.put("dateNotificationArret", LocalDate.now().plusDays(1).toString());
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── Isolation workspace ─────────────────────────────────────────────

    @Test
    void POST_autreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(url(dtBeCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyRecommande())))
                .andExpect(status().isNotFound());
    }

    // ── GET / upsert ────────────────────────────────────────────────────

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(url(dtFrCf)).with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_deuxFois_puisGET_upsertRemplaceAnalyse() throws Exception {
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyRecommande())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("POURVOI_RECOMMANDE"));

        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyRisque())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("POURVOI_RISQUE"));

        mockMvc.perform(get(url(dtFrCf)).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("POURVOI_RISQUE"))
                .andExpect(jsonPath("$.risqueNonAdmission").value("MODERE"));
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private String url(CaseFile cf) {
        return "/api/v1/case-files/" + cf.getId() + "/pourvoi-cassation-soc-analysis";
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
