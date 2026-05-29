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
 * SF-214-35 : tests d'intégration de {@link AssignationResidenceController}.
 * Couvre POST/GET, gates country + domaine, durée &gt; 135 j, durée ≤ 0, upsert,
 * 404 et isolation workspace.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class AssignationResidenceControllerIT {

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

        User uFr = save(new User(), u -> { u.setEmail("asgr-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-asgr-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI-ASGR " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI-ASGR " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-asgr-fr-" + ts, "asgr-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("asgr-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-asgr-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI-ASGR " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI-ASGR " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-asgr-be-" + ts, "asgr-be-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("asgr-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-asgr-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT-ASGR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT-ASGR " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-asgr-dt-" + ts, "asgr-dt-" + ts + "@ex.com");
    }

    // ── Helpers de corps de requête ─────────────────────────────────────

    /** Notifiée il y a 10 j pour 45 j → échéance dans 35 j → EN_COURS. */
    private Map<String, Object> bodyEnCours() {
        return body(LocalDate.now().minusDays(10).toString(), 45,
                "EXECUTION_OQTF", "Pointage quotidien gendarmerie");
    }

    private Map<String, Object> body(String dateNotif, int duree, String motif, String obligations) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateNotificationAssignation", dateNotif);
        m.put("dureeAssignationJours", duree);
        m.put("motifAssignation", motif);
        if (obligations != null) m.put("obligationsPresentation", obligations);
        return m;
    }

    // ── Tests nominaux ──────────────────────────────────────────────────

    @Test
    void POST_fr_enCours_retourne200_avecEcheanceEtContestation() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEnCours())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.statut").value("EN_COURS"))
                .andExpect(jsonPath("$.dateEcheanceAssignation").exists())
                .andExpect(jsonPath("$.dureeTotaleAutoriseeJours").value(135))
                .andExpect(jsonPath("$.renouvellementPossible").value(true))
                .andExpect(jsonPath("$.recoursPossible").value(true))
                .andExpect(jsonPath("$.delaiRecoursTaHeures").value(48))
                .andExpect(jsonPath("$.motifsContestation", org.hamcrest.Matchers.hasSize(4)))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("L. 731-1")));
    }

    // ── Gates ───────────────────────────────────────────────────────────

    @Test
    void POST_workspaceBelgique_returns400() throws Exception {
        mockMvc.perform(post(url(immBeCf)).with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEnCours())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEnCours())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dureeSuperieureA135_returns400() throws Exception {
        Map<String, Object> b = body(LocalDate.now().minusDays(1).toString(), 136,
                "EXECUTION_OQTF", null);
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dureeNulle_returns400() throws Exception {
        Map<String, Object> b = body(LocalDate.now().minusDays(1).toString(), 0,
                "EXECUTION_OQTF", null);
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
                        .content(objectMapper.writeValueAsString(bodyEnCours())))
                .andExpect(status().isNotFound());
    }

    // ── Upsert + GET ────────────────────────────────────────────────────

    @Test
    void GET_afterPost_retourneAnalysePersistee() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEnCours())))
                .andExpect(status().isOk());

        mockMvc.perform(get(url(immFrCf)).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.statut").value("EN_COURS"))
                .andExpect(jsonPath("$.motifAssignation").value("EXECUTION_OQTF"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(url(immFrCf)).with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private String url(CaseFile cf) {
        return "/api/v1/case-files/" + cf.getId() + "/assignation-residence-analysis";
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
