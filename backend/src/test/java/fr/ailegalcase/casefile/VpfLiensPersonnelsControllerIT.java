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

/**
 * SF-214-05 : tests d'intégration de {@link VpfLiensPersonnelsController}.
 * Couvre POST/GET, gates country + domaine, durée négative, upsert, 404 et
 * isolation workspace.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class VpfLiensPersonnelsControllerIT {

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

        User uFr = save(new User(), u -> { u.setEmail("vpf-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-vpf-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI-VPF " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI-VPF " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-vpf-fr-" + ts, "vpf-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("vpf-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-vpf-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI-VPF " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI-VPF " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-vpf-be-" + ts, "vpf-be-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("vpf-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-vpf-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT-VPF " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT-VPF " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-vpf-dt-" + ts, "vpf-dt-" + ts + "@ex.com");
    }

    // ── Helpers de corps de requête ─────────────────────────────────────

    private Map<String, Object> bodyEligibleProbable() {
        // 72 mois, enfants + conjoint (+4), intégration FORT (+1), pas de condamnation
        return body(72, false, true, true, false, null, "FORT", false);
    }

    private Map<String, Object> bodyAConsolider() {
        // 24 mois (< 5 ans, pas entrée mineure) → durée non remplie
        return body(24, false, false, true, false, null, "FAIBLE", false);
    }

    private Map<String, Object> body(int duree, boolean mineur, boolean enfants,
                                     boolean conjoint, boolean parents, String situation,
                                     String niveau, boolean condamnation) {
        Map<String, Object> m = new HashMap<>();
        m.put("dureeResidenceFranceMois", duree);
        m.put("entreeEnFranceMineur", mineur);
        m.put("enfantsEnFrance", enfants);
        m.put("conjointEnFrance", conjoint);
        m.put("parentsEnFrance", parents);
        if (situation != null) m.put("situationFamilialeAlEtranger", situation);
        m.put("niveauIntegration", niveau);
        m.put("ancienneConvictionPenale", condamnation);
        return m;
    }

    // ── Tests nominaux ──────────────────────────────────────────────────

    @Test
    void POST_fr_eligibleProbable_retourne200_avecScoreEtRecommandations() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleProbable())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_PROBABLE"))
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.dureeResidenceRemplie").value(true))
                .andExpect(jsonPath("$.recommandations").isArray())
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("L. 423-23")));
    }

    @Test
    void POST_fr_aConsolider_retourneChipsEtRecommandations() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAConsolider())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DOSSIER_A_CONSOLIDER"))
                .andExpect(jsonPath("$.chipsCriteresNonRemplis",
                        org.hamcrest.Matchers.hasItem("DUREE_RESIDENCE_INSUFFISANTE")))
                .andExpect(jsonPath("$.recommandations",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())));
    }

    // ── Gates ───────────────────────────────────────────────────────────

    @Test
    void POST_workspaceBelgique_returns400() throws Exception {
        mockMvc.perform(post(url(immBeCf)).with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleProbable())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleProbable())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dureeNegative_returns400() throws Exception {
        Map<String, Object> body = bodyEligibleProbable();
        body.put("dureeResidenceFranceMois", -3);
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
                        .content(objectMapper.writeValueAsString(bodyEligibleProbable())))
                .andExpect(status().isNotFound());
    }

    // ── Upsert + GET ────────────────────────────────────────────────────

    @Test
    void POST_deuxFois_upsert_retourneDernierResultat() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleProbable())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_PROBABLE"));

        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAConsolider())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DOSSIER_A_CONSOLIDER"));
    }

    @Test
    void GET_afterPost_retourneAnalysePersistee() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleProbable())))
                .andExpect(status().isOk());

        mockMvc.perform(get(url(immFrCf)).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_PROBABLE"))
                .andExpect(jsonPath("$.niveauIntegration").value("FORT"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(url(immFrCf)).with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private String url(CaseFile cf) {
        return "/api/v1/case-files/" + cf.getId() + "/vpf-liens-personnels-analysis";
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
