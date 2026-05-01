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

import java.math.BigDecimal;
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
 * F-167 SF-167-01 — Test d'intégration sur l'endpoint dashboard. Vérifie que :
 * <ul>
 *   <li>Sans analyse persistée, {@code tiles} est une liste vide.</li>
 *   <li>Après création d'une {@code ChangementStatutAnalysis}, la tile
 *       {@code F-IM-11-changement-statut} apparaît dans le dashboard.</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class CaseFileDashboardControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User u = save(new User(), x -> { x.setEmail("dash-fr-" + ts + "@ex.com"); x.setStatus("ACTIVE"); });
        saveAuth(u, "g-dash-fr-" + ts);
        Workspace ws = saveWs(u, "WS-DASH " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(u, ws);
        caseFile = saveCf(u, ws, "CF-DASH " + ts, "DROIT_IMMIGRATION");
        auth = buildAuth("g-dash-fr-" + ts, "dash-fr-" + ts + "@ex.com");
    }

    @Test
    void getDashboard_returnsEmptyTilesWhenNoAnalysis() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFile.getId() + "/dashboard")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiles").isArray())
                .andExpect(jsonPath("$.tiles.length()").value(0));
    }

    @Test
    void getDashboard_includesChangementStatutTile_whenAnalysisExists() throws Exception {
        // 1. Crée l'analyse via l'endpoint POST existant.
        Map<String, Object> body = new HashMap<>();
        body.put("titreActuel", "ETUDIANT");
        body.put("titreEnvisage", "VPF");
        body.put("dureeRestanteSurTitreActuelMois", 8);
        body.put("documentJustificatifFourni", true);
        body.put("remunerationContratEur", new BigDecimal("2800.00"));
        body.put("casierJudiciaireVierge", true);

        mockMvc.perform(post("/api/v1/case-files/" + caseFile.getId() + "/changement-statut-analysis")
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictTransition").value("ELEVEE"));

        // 2. GET dashboard → tiles contient F-IM-11.
        mockMvc.perform(get("/api/v1/case-files/" + caseFile.getId() + "/dashboard")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiles").isArray())
                .andExpect(jsonPath(
                        "$.tiles[?(@.toolId == 'F-IM-11-changement-statut')].theme")
                        .value("VALIDITE"))
                .andExpect(jsonPath(
                        "$.tiles[?(@.toolId == 'F-IM-11-changement-statut')].label")
                        .value("Changement de statut"))
                .andExpect(jsonPath(
                        "$.tiles[?(@.toolId == 'F-IM-11-changement-statut')].alertLevel")
                        .value("OK"));
    }

    // ---- helpers (copiés du pattern ChangementStatutControllerIT) ----

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
