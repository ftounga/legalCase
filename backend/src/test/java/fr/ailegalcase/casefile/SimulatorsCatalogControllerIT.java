package fr.ailegalcase.casefile;

import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SF-163-01 — tests d'intégration de l'endpoint GET /api/v1/simulators.
 *
 * <p>Vérifie que le catalogue retourné reflète bien le domain × country du
 * workspace primary de l'utilisateur authentifié, que les outils sont issus
 * de la table {@code decision_tool_visibility_rules} (déjà seedée), et que
 * l'isolation cross-workspace est respectée.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class SimulatorsCatalogControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authImmFr;
    private OAuth2AuthenticationToken authFamilleBe;
    private OAuth2AuthenticationToken authTravailFr;
    private OAuth2AuthenticationToken authNoPrimaryWorkspace;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace 1 : DROIT_IMMIGRATION / FRANCE (primary pour authImmFr)
        authImmFr = createUserWithWorkspace(
                "sim-imm-fr-" + ts,
                "DROIT_IMMIGRATION",
                "FRANCE");

        // Workspace 2 : DROIT_FAMILLE / BELGIQUE (primary pour authFamilleBe)
        authFamilleBe = createUserWithWorkspace(
                "sim-fa-be-" + ts,
                "DROIT_FAMILLE",
                "BELGIQUE");

        // Workspace 3 : DROIT_DU_TRAVAIL / FRANCE (primary pour authTravailFr)
        // Sert à vérifier l'isolation — un user Travail FR ne doit pas voir les outils Immigration FR.
        authTravailFr = createUserWithWorkspace(
                "sim-tr-fr-" + ts,
                "DROIT_DU_TRAVAIL",
                "FRANCE");

        // User sans aucun workspace primary : on crée l'auth account, pas de workspace.
        // C'est le seul cas où le service peut renvoyer legalDomain=null (la colonne
        // workspaces.legal_domain étant NOT NULL côté DB, un workspace sans domaine
        // est impossible).
        authNoPrimaryWorkspace = createUserWithoutWorkspace("sim-no-ws-" + ts);
    }

    // ──────────────────────────── IT-01 : Immigration FR ─────────────────────

    @Test
    void IT01_immigrationFr_returns200_avecToolIds() throws Exception {
        mockMvc.perform(get("/api/v1/simulators")
                        .with(authentication(authImmFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalDomain").value("DROIT_IMMIGRATION"))
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.toolIds").isArray())
                .andExpect(jsonPath("$.toolIds", Matchers.hasItem("F-IM-05-arbre-decisionnel-titre")))
                .andExpect(jsonPath("$.toolIds", Matchers.hasItem("F-IM-06-recours")))
                .andExpect(jsonPath("$.toolIds", Matchers.hasItem("F-IM-08-oqtf-avec-delai-fr")));
    }

    // ──────────────────────────── IT-02 : Famille BE ─────────────────────────

    @Test
    void IT02_familleBe_returns200_avecToolIds() throws Exception {
        mockMvc.perform(get("/api/v1/simulators")
                        .with(authentication(authFamilleBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalDomain").value("DROIT_FAMILLE"))
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.toolIds").isArray())
                // F-FA-11-desunion-irremediable-be est seedée pour DROIT_FAMILLE / BELGIQUE.
                .andExpect(jsonPath("$.toolIds", Matchers.hasItem("F-FA-11-desunion-irremediable-be")));
    }

    // ──────────────────────────── IT-03 : 401 sans auth ──────────────────────

    @Test
    void IT03_sansAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/simulators"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────── IT-04 : pas de workspace primary ───────────

    @Test
    void IT04_userSansWorkspacePrimary_returns200_toolIdsVide() throws Exception {
        // Garde-fou : si l'utilisateur authentifié n'a pas (encore) de workspace primary,
        // le service répond 200 avec un catalogue vide plutôt que 500. Cas rare en prod
        // (création de compte en cours) mais nominal côté contrat API.
        mockMvc.perform(get("/api/v1/simulators")
                        .with(authentication(authNoPrimaryWorkspace)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalDomain").doesNotExist())
                .andExpect(jsonPath("$.country").doesNotExist())
                .andExpect(jsonPath("$.toolIds").isArray())
                .andExpect(jsonPath("$.toolIds").isEmpty());
    }

    // ──────────────────────────── IT-05 : Isolation cross-domaine ────────────

    @Test
    void IT05_isolation_userTravailFr_neVoitPas_outilsImmigration() throws Exception {
        // Un user Travail FR ne doit pas voir les outils Immigration FR dans son catalogue.
        mockMvc.perform(get("/api/v1/simulators")
                        .with(authentication(authTravailFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalDomain").value("DROIT_DU_TRAVAIL"))
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.toolIds").isArray())
                .andExpect(jsonPath("$.toolIds",
                        Matchers.not(Matchers.hasItem("F-IM-05-arbre-decisionnel-titre"))))
                .andExpect(jsonPath("$.toolIds",
                        Matchers.not(Matchers.hasItem("F-IM-06-recours"))))
                .andExpect(jsonPath("$.toolIds",
                        Matchers.not(Matchers.hasItem("F-IM-08-oqtf-avec-delai-fr"))));
    }

    // ─────────────────────────── Helpers ────────────────────────────────────

    private OAuth2AuthenticationToken createUserWithoutWorkspace(String suffix) {
        User u = new User();
        u.setEmail(suffix + "@ex.com");
        u.setStatus("ACTIVE");
        u = userRepository.save(u);

        AuthAccount a = new AuthAccount();
        a.setUser(u);
        a.setProvider("GOOGLE");
        a.setProviderUserId("g-" + suffix);
        authAccountRepository.save(a);

        return buildAuth("g-" + suffix, suffix + "@ex.com");
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
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
