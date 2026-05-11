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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SF-163-03 — tests d'intégration du dispatcher
 * {@code POST /api/v1/simulators/{toolId}/calculate}.
 *
 * <p>Vérifie : (a) nominal F-DT-08, (b) 3 outils représentatifs (Famille FR
 * / Immigration FR / Travail BE), (c) toolId inconnu 404, (d) payload invalide
 * 400 / 422, (e) 401 sans auth, (f) non-persistance vérifiée par compteurs DB.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class SimulatorCalculateControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authTravailFr;
    private OAuth2AuthenticationToken authTravailBe;
    private OAuth2AuthenticationToken authFamilleFr;
    private OAuth2AuthenticationToken authImmFr;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        authTravailFr = createUserWithWorkspace("sim-tr-fr-" + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        authTravailBe = createUserWithWorkspace("sim-tr-be-" + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        authFamilleFr = createUserWithWorkspace("sim-fa-fr-" + ts, "DROIT_FAMILLE", "FRANCE");
        authImmFr = createUserWithWorkspace("sim-imm-fr-" + ts, "DROIT_IMMIGRATION", "FRANCE");
    }

    // ─────────────────────── CA-04 + CA-05 + CA-11(a) ───────────────────────

    @Test
    void licenciementValidity_nominal_200() throws Exception {
        Map<String, Object> body = Map.of(
                "country", "FRANCE",
                "reponses", Map.of(
                        "FR_CONVOCATION", "OUI",
                        "FR_ENTRETIEN", "OUI",
                        "FR_DELAI_NOTIFICATION", "OUI",
                        "FR_MOTIVATION", "OUI",
                        "FR_MOTIF_REEL", "OUI",
                        "FR_PROCEDURE_DISCIPLINAIRE", "OUI",
                        "FR_ORDRE_LICENCIEMENT", "OUI"));

        mockMvc.perform(post("/api/v1/simulators/F-DT-08-licenciement-validity/calculate")
                        .with(authentication(authTravailFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("VALIDE"))
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    // ─────────────────────── CA-06 : Tool inconnu ───────────────────────────

    @Test
    void unknownToolId_404() throws Exception {
        mockMvc.perform(post("/api/v1/simulators/F-FAKE-UNKNOWN/calculate")
                        .with(authentication(authTravailFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Tool not supported in simulator mode"))
                .andExpect(jsonPath("$.toolId").value("F-FAKE-UNKNOWN"));
    }

    // ─────────────────────── CA-07 : JSON invalide ──────────────────────────

    @Test
    void invalidJson_400() throws Exception {
        mockMvc.perform(post("/api/v1/simulators/F-DT-08-licenciement-validity/calculate")
                        .with(authentication(authTravailFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not a valid json"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────── CA-08 : Champ manquant → 422 ───────────────────

    @Test
    void missingRequiredField_422() throws Exception {
        // F-DT-11 requiert salaireMensuelReference + motifNullite — on n'envoie
        // ni l'un ni l'autre, le calculator lève IllegalArgumentException → 422.
        mockMvc.perform(post("/api/v1/simulators/F-DT-11-harcelement-licenciement-nul/calculate")
                        .with(authentication(authTravailFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ─────────────────────── CA-09 : 401 sans auth ──────────────────────────

    @Test
    void unauthenticated_401() throws Exception {
        mockMvc.perform(post("/api/v1/simulators/F-DT-08-licenciement-validity/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── CA-10 : Non-persistance ────────────────────────

    @Test
    void noPersistence_databaseUnchanged() throws Exception {
        long licBefore = countRows("licenciement_analyses");
        long rupBefore = countRows("rupture_conv_analyses");
        long discBefore = countRows("discrimination_analyses");
        long harcBefore = countRows("harcelement_nullite_analyses");
        long cfBefore = countRows("case_files");

        Map<String, Object> body = Map.of(
                "country", "FRANCE",
                "reponses", Map.of("FR_CONVOCATION", "OUI"));

        mockMvc.perform(post("/api/v1/simulators/F-DT-08-licenciement-validity/calculate")
                        .with(authentication(authTravailFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Vérifie : zéro insertion dans 5 tables d'analyses + case_files.
        long licAfter = countRows("licenciement_analyses");
        long rupAfter = countRows("rupture_conv_analyses");
        long discAfter = countRows("discrimination_analyses");
        long harcAfter = countRows("harcelement_nullite_analyses");
        long cfAfter = countRows("case_files");

        org.junit.jupiter.api.Assertions.assertEquals(licBefore, licAfter,
                "licenciement_analyses : insertion détectée");
        org.junit.jupiter.api.Assertions.assertEquals(rupBefore, rupAfter,
                "rupture_conv_analyses : insertion détectée");
        org.junit.jupiter.api.Assertions.assertEquals(discBefore, discAfter,
                "discrimination_analyses : insertion détectée");
        org.junit.jupiter.api.Assertions.assertEquals(harcBefore, harcAfter,
                "harcelement_nullite_analyses : insertion détectée");
        org.junit.jupiter.api.Assertions.assertEquals(cfBefore, cfAfter,
                "case_files : insertion détectée");
    }

    // ─────────────────────── CA-11(b) : Famille FR ──────────────────────────

    @Test
    void familleFR_nominal_200() throws Exception {
        // F-FA-05 partage immobilier : payload minimal.
        Map<String, Object> body = Map.of(
                "country", "FRANCE",
                "valeurVenale", 300000,
                "capitalRestantDu", 100000,
                "quotePartAttributaire", 0.5,
                "isDivorce", true);

        mockMvc.perform(post("/api/v1/simulators/F-FA-05-partage-immobilier/calculate")
                        .with(authentication(authFamilleFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.valeurNette").exists());
    }

    // ─────────────────────── CA-11(b) : Immigration FR ──────────────────────

    @Test
    void immigrationFR_nominal_200() throws Exception {
        // F-IM-13 naturalisation FR — voie DECRET.
        Map<String, Object> body = Map.of(
                "voieNaturalisation", "DECRET",
                "dureeResidenceReguliereAnnees", 6,
                "ageDemandeur", 35,
                "casierJudiciaireVierge", true,
                "assimilationLangueB1", true,
                "ressourcesStables", true);

        mockMvc.perform(post("/api/v1/simulators/F-IM-13-naturalisation/calculate")
                        .with(authentication(authImmFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecevabilite").exists());
    }

    // ─────────────────────── CA-11(b) : Travail BE ──────────────────────────

    @Test
    void travailBE_nominal_200() throws Exception {
        // F-DT-27 motif grave BE — payload minimal.
        Map<String, Object> body = Map.of(
                "dateConnaissanceFait", "2026-04-01",
                "dateNotificationRupture", "2026-04-02",
                "dateNotificationMotifs", "2026-04-04",
                "anciennetteAnnees", 5,
                "salaireMensuelReference", 3000);

        mockMvc.perform(post("/api/v1/simulators/F-DT-27-motif-grave-be/calculate")
                        .with(authentication(authTravailBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formule").exists());
    }

    // ─────────────────────── Helpers ────────────────────────────────────────

    private long countRows(String table) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table, Long.class);
        return count != null ? count : 0L;
    }

    private OAuth2AuthenticationToken createUserWithWorkspace(String suffix,
                                                              String legalDomain,
                                                              String country) {
        User u = new User();
        u.setEmail(suffix + "@ex.com");
        u.setStatus("ACTIVE");
        u = userRepository.save(u);

        AuthAccount a = new AuthAccount();
        a.setUser(u);
        a.setProvider("GOOGLE");
        a.setProviderUserId("g-" + suffix);
        authAccountRepository.save(a);

        Workspace ws = new Workspace();
        ws.setName("WS " + suffix);
        ws.setSlug("ws-" + suffix);
        ws.setOwner(u);
        ws.setLegalDomain(legalDomain);
        ws.setCountry(country);
        ws.setPlanCode("STARTER");
        ws.setStatus("ACTIVE");
        ws = workspaceRepository.save(ws);

        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(ws);
        m.setUser(u);
        m.setMemberRole("OWNER");
        m.setPrimary(true);
        workspaceMemberRepository.save(m);

        return buildAuth("g-" + suffix, suffix + "@ex.com");
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
