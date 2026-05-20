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
 * SF-207-04 : tests d'intégration de l'endpoint déclaration AT Fedris (Travail BE).
 *
 * <p>Couvre 7 cas mini-spec : POST BE 200 (prospectif), POST BE 200 (rétrospectif),
 * POST FR 404 (isolation pays), POST caseFile autre workspace 404 (isolation
 * workspace), validation Bean 400 (dateAccident manquant), validation service
 * 400 (dateConnaissance &lt; dateAccident), GET après POST, GET sans POST 404.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class AtFedrisDeclarationControllerIT {

    private static final String URL = "/api/v1/case-files/%s/decision-tools/at-fedris-declaration";

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
    private CaseFile beCaseFile;
    private CaseFile frCaseFile;
    private CaseFile beOtherWorkspaceCaseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace BE DROIT_DU_TRAVAIL → cible
        User uBe = save(new User(), u -> { u.setEmail("atfd-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-atfd-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-atfd-be-" + ts, "atfd-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("atfd-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-atfd-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-atfd-fr-" + ts, "atfd-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace (avocat BE essaie POST sur dossier d'un autre workspace BE)
        User uBe2 = save(new User(), u -> { u.setEmail("atfd-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-atfd-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 " + ts, "DROIT_DU_TRAVAIL");
    }

    private Map<String, Object> body(String dateAccident,
                                     String dateConnaissance,
                                     String dateAction,
                                     String dateDeclaration) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateAccident", dateAccident);
        m.put("dateConnaissanceEmployeur", dateConnaissance);
        m.put("dateActionEnvisagee", dateAction);
        m.put("dateDeclarationEffectuee", dateDeclaration);
        return m;
    }

    @Test
    void POST_workspaceBe_modeProspectif_returns200AndPersists() throws Exception {
        // dateAccident = 2026-05-10 → dateLimite = 2026-05-18
        // dateAction = 2026-05-13 → joursRestants = 5 → DELAI_OUVERT
        Map<String, Object> body = body("2026-05-10", null, "2026-05-13", null);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.verdict").value("DELAI_OUVERT"))
                .andExpect(jsonPath("$.dateLimiteDeclaration").value("2026-05-18"))
                .andExpect(jsonPath("$.joursRestants").value(5))
                .andExpect(jsonPath("$.regleAppliquee").value("8_JOURS_LOI_1971_ART_62"))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("10 avril 1971")))
                .andExpect(jsonPath("$.formuleCalcul",
                        org.hamcrest.Matchers.containsString("8 jours")))
                .andExpect(jsonPath("$.consequencesNonRespect",
                        org.hamcrest.Matchers.containsString("Préjudice")));
    }

    @Test
    void POST_workspaceBe_modeRetrospectif_returns200() throws Exception {
        // dateAccident = 2026-05-01 → dateLimite = 2026-05-09
        // dateDeclaration = 2026-05-07 → DECLARATION_DANS_LES_TEMPS (joursRestants = 2)
        Map<String, Object> body = body("2026-05-01", null, null, "2026-05-07");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DECLARATION_DANS_LES_TEMPS"))
                .andExpect(jsonPath("$.joursRestants").value(2))
                .andExpect(jsonPath("$.dateDeclarationEffectuee").value("2026-05-07"));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        // Avocat FR essaie l'endpoint sur son propre dossier FR → 404
        // (gate BE strict, l'outil n'existe pas côté FR)
        Map<String, Object> body = body("2026-05-10", null, "2026-05-13", null);
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        // Avocat BE essaie d'accéder à un caseFile d'un autre workspace BE → 404
        Map<String, Object> body = body("2026-05-10", null, "2026-05-13", null);
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dateAccidentManquant_returns400() throws Exception {
        // dateAccident absent → @NotNull Bean Validation → 400
        Map<String, Object> body = body(null, null, "2026-05-13", null);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateConnaissanceAvantDateAccident_returns400() throws Exception {
        // dateConnaissance strictement antérieure à dateAccident → 400 service
        Map<String, Object> body = body("2026-05-10", "2026-05-09", null, null);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        Map<String, Object> body = body("2026-05-10", "2026-05-11", "2026-05-15", null);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.dateAccident").value("2026-05-10"))
                .andExpect(jsonPath("$.dateConnaissanceEmployeur").value("2026-05-11"))
                .andExpect(jsonPath("$.dateLimiteDeclaration").value("2026-05-19"))
                .andExpect(jsonPath("$.regleAppliquee").value("8_JOURS_LOI_1971_ART_62"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    // ---- helpers (alignés sur PrescriptionBeLitigeTravailControllerIT) ----

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
