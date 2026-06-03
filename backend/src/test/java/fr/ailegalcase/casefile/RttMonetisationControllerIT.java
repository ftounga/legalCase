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
 * SF-218-37 : tests d'intégration de {@link RttMonetisationController} (F-DT-51,
 * outil FRANCE uniquement). Couvre POST/GET nominaux, gate country (BE → 400) +
 * domaine (DROIT_IMMIGRATION → 400), validations (400), isolation workspace
 * (404), GET sans POST (404) et upsert.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class RttMonetisationControllerIT {

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

        User uFr = save(new User(), u -> { u.setEmail("rtt-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-rtt-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRT-RTT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        dtFrCf = saveCf(uFr, wsFr, "CFRT-RTT " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-rtt-fr-" + ts, "rtt-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("rtt-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-rtt-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBET-RTT " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        dtBeCf = saveCf(uBe, wsBe, "CBET-RTT " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-rtt-be-" + ts, "rtt-be-" + ts + "@ex.com");

        User uImm = save(new User(), u -> { u.setEmail("rtt-imm-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uImm, "g-rtt-imm-" + ts);
        Workspace wsImm = saveWs(uImm, "WSFRI-RTT " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uImm, wsImm);
        immFrCf = saveCf(uImm, wsImm, "CFRI-RTT " + ts, "DROIT_IMMIGRATION");
        authImm = buildAuth("g-rtt-imm-" + ts, "rtt-imm-" + ts + "@ex.com");
    }

    // ── Helpers de corps de requête ─────────────────────────────────────

    /** Nominal éligible : 5 jours, 200 €/j, taux défaut, jours dans la fenêtre. */
    private Map<String, Object> bodyEligible() {
        Map<String, Object> m = new HashMap<>();
        m.put("nombreJoursRttRenonces", 5);
        m.put("salaireJournalierBrut", 200);
        m.put("joursAcquisDansFenetre", true);
        return m;
    }

    // ── Tests nominaux ──────────────────────────────────────────────────

    @Test
    void POST_fr_eligible_retourne200_montantExact() throws Exception {
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.statut").value("ELIGIBLE"))
                .andExpect(jsonPath("$.tauxApplique").value(25.0))
                .andExpect(jsonPath("$.montantBrut").value(1250.00))
                .andExpect(jsonPath("$.regimeSocialFiscal").value("ALIGNE_HEURES_SUPPLEMENTAIRES"))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("2022-1157")));
    }

    @Test
    void POST_fr_tauxPlafonne_a25() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("tauxMajorationConventionnel", 40);
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ELIGIBLE"))
                .andExpect(jsonPath("$.tauxApplique").value(25.0));
    }

    @Test
    void POST_fr_horsFenetre_nonEligible_sansMontant() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("joursAcquisDansFenetre", false);
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("NON_ELIGIBLE"))
                .andExpect(jsonPath("$.montantBrut").doesNotExist());
    }

    // ── Validations / gates ─────────────────────────────────────────────

    @Test
    void POST_nombreJoursNul_returns400() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("nombreJoursRttRenonces", 0);
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salaireNul_returns400() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("salaireJournalierBrut", 0);
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_champRequisNull_returns400() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("joursAcquisDansFenetre", null);
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_workspaceBelgique_returns400() throws Exception {
        mockMvc.perform(post(url(dtBeCf)).with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitImmigration_returns400() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authImm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isBadRequest());
    }

    // ── Isolation workspace ─────────────────────────────────────────────

    @Test
    void POST_autreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(url(dtBeCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
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
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ELIGIBLE"));

        Map<String, Object> nonEligible = bodyEligible();
        nonEligible.put("joursAcquisDansFenetre", false);
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonEligible)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("NON_ELIGIBLE"));

        mockMvc.perform(get(url(dtFrCf)).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("NON_ELIGIBLE"))
                .andExpect(jsonPath("$.montantBrut").doesNotExist());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private String url(CaseFile cf) {
        return "/api/v1/case-files/" + cf.getId() + "/rtt-monetisation-analysis";
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
