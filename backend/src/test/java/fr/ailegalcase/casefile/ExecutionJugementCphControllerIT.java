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
 * SF-218-03 : tests d'intégration de {@link ExecutionJugementCphController}.
 * Couvre POST/GET, gates country (BE → 400) + domaine (DROIT_IMMIGRATION →
 * 400), procédure collective sans date d'ouverture (400), montant ≤ 0 (400),
 * isolation workspace (404), GET sans POST (404) et upsert.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ExecutionJugementCphControllerIT {

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

        User uFr = save(new User(), u -> { u.setEmail("ejcph-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-ejcph-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRT-EJCPH " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        dtFrCf = saveCf(uFr, wsFr, "CFRT-EJCPH " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-ejcph-fr-" + ts, "ejcph-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("ejcph-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-ejcph-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBET-EJCPH " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        dtBeCf = saveCf(uBe, wsBe, "CBET-EJCPH " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-ejcph-be-" + ts, "ejcph-be-" + ts + "@ex.com");

        User uImm = save(new User(), u -> { u.setEmail("ejcph-imm-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uImm, "g-ejcph-imm-" + ts);
        Workspace wsImm = saveWs(uImm, "WSFRI-EJCPH " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uImm, wsImm);
        immFrCf = saveCf(uImm, wsImm, "CFRI-EJCPH " + ts, "DROIT_IMMIGRATION");
        authImm = buildAuth("g-ejcph-imm-" + ts, "ejcph-imm-" + ts + "@ex.com");
    }

    // ── Helpers de corps de requête ─────────────────────────────────────

    private Map<String, Object> bodyInBonis() {
        Map<String, Object> m = new HashMap<>();
        m.put("dateJugement", LocalDate.now().minusDays(10).toString());
        m.put("montantCondamnation", 18_000.0);
        m.put("executionProvisoireOrdonnee", true);
        m.put("situationEmployeur", "IN_BONIS");
        m.put("ancienneteContratMois", 36);
        return m;
    }

    private Map<String, Object> bodyLiquidation() {
        Map<String, Object> m = bodyInBonis();
        m.put("situationEmployeur", "LIQUIDATION");
        m.put("dateOuvertureProcedureCollective", LocalDate.now().minusDays(40).toString());
        return m;
    }

    // ── Tests nominaux ──────────────────────────────────────────────────

    @Test
    void POST_fr_inBonis_retourne200_executionDirecte() throws Exception {
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyInBonis())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdict").value("EXECUTION_DIRECTE"))
                .andExpect(jsonPath("$.agsEligible").value(false))
                .andExpect(jsonPath("$.checklist").isArray())
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("514")));
    }

    @Test
    void POST_fr_liquidation_relaisAgs_plafondCalcule() throws Exception {
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyLiquidation())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RELAIS_AGS"))
                .andExpect(jsonPath("$.agsEligible").value(true))
                .andExpect(jsonPath("$.agsCoefficientPlafond").value(6))
                .andExpect(jsonPath("$.agsPlafondEuros").isNumber());
    }

    // ── Gates ───────────────────────────────────────────────────────────

    @Test
    void POST_workspaceBelgique_returns400() throws Exception {
        mockMvc.perform(post(url(dtBeCf)).with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyInBonis())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitImmigration_returns400() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authImm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyInBonis())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_procedureCollectiveSansDate_returns400() throws Exception {
        Map<String, Object> body = bodyInBonis();
        body.put("situationEmployeur", "REDRESSEMENT");
        // pas de dateOuvertureProcedureCollective
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_montantZero_returns400() throws Exception {
        Map<String, Object> body = bodyInBonis();
        body.put("montantCondamnation", 0.0);
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateFuture_returns400() throws Exception {
        Map<String, Object> body = bodyInBonis();
        body.put("dateJugement", LocalDate.now().plusDays(1).toString());
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
                        .content(objectMapper.writeValueAsString(bodyInBonis())))
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
                        .content(objectMapper.writeValueAsString(bodyInBonis())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("EXECUTION_DIRECTE"));

        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyLiquidation())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RELAIS_AGS"));

        mockMvc.perform(get(url(dtFrCf)).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RELAIS_AGS"))
                .andExpect(jsonPath("$.agsEligible").value(true));
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private String url(CaseFile cf) {
        return "/api/v1/case-files/" + cf.getId() + "/execution-jugement-cph-analysis";
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
