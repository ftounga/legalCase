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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SF-238-03 : tests d'intégration pour les endpoints d'activation manuelle
 * d'outils décisionnels + injection dans
 * {@link DecisionToolVisibilityService#resolveVisibleTools}.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ManualToolActivationControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ManualToolActivationRepository manualToolActivationRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authMain;
    private OAuth2AuthenticationToken authOther;
    private CaseFile mainCf;
    private CaseFile otherWsCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace principal : DROIT_DU_TRAVAIL / FRANCE
        User u1 = new User();
        u1.setEmail("mta-" + ts + "@ex.com");
        u1.setStatus("ACTIVE");
        u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount();
        a1.setUser(u1);
        a1.setProvider("GOOGLE");
        a1.setProviderUserId("g-mta-" + ts);
        authAccountRepository.save(a1);
        Workspace ws1 = new Workspace();
        ws1.setName("MTA " + ts);
        ws1.setSlug("ws-mta-" + ts);
        ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_DU_TRAVAIL");
        ws1.setCountry("FRANCE");
        ws1.setPlanCode("STARTER");
        ws1.setStatus("ACTIVE");
        ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember();
        m1.setWorkspace(ws1);
        m1.setUser(u1);
        m1.setMemberRole("OWNER");
        m1.setPrimary(true);
        workspaceMemberRepository.save(m1);
        mainCf = new CaseFile();
        mainCf.setTitle("MTA " + ts);
        mainCf.setWorkspace(ws1);
        mainCf.setCreatedBy(u1);
        mainCf.setLegalDomain("DROIT_DU_TRAVAIL");
        mainCf.setStatus("OPEN");
        mainCf = caseFileRepository.save(mainCf);
        authMain = buildAuth("g-mta-" + ts, "mta-" + ts + "@ex.com");

        // Workspace tiers : isolation
        User u2 = new User();
        u2.setEmail("mta-o-" + ts + "@ex.com");
        u2.setStatus("ACTIVE");
        u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount();
        a2.setUser(u2);
        a2.setProvider("GOOGLE");
        a2.setProviderUserId("g-mta-o-" + ts);
        authAccountRepository.save(a2);
        Workspace ws2 = new Workspace();
        ws2.setName("MTA O " + ts);
        ws2.setSlug("ws-mta-o-" + ts);
        ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_DU_TRAVAIL");
        ws2.setCountry("FRANCE");
        ws2.setPlanCode("STARTER");
        ws2.setStatus("ACTIVE");
        ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember();
        m2.setWorkspace(ws2);
        m2.setUser(u2);
        m2.setMemberRole("OWNER");
        m2.setPrimary(true);
        workspaceMemberRepository.save(m2);
        otherWsCf = new CaseFile();
        otherWsCf.setTitle("MTA O " + ts);
        otherWsCf.setWorkspace(ws2);
        otherWsCf.setCreatedBy(u2);
        otherWsCf.setLegalDomain("DROIT_DU_TRAVAIL");
        otherWsCf.setStatus("OPEN");
        otherWsCf = caseFileRepository.save(otherWsCf);
        authOther = buildAuth("g-mta-o-" + ts, "mta-o-" + ts + "@ex.com");
    }

    // ──────────────────────────── Nominal ───────────────────────────────────

    @Test
    void POST_active_un_outil_retourne_200_et_persiste() throws Exception {
        String body = "{\"toolId\": \"F-DT-10-rupture-conv-validity\"}";

        mockMvc.perform(post(url(mainCf.getId().toString()))
                        .with(authentication(authMain))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.toolId").value("F-DT-10-rupture-conv-validity"))
                .andExpect(jsonPath("$.activatedAt").exists());

        List<ManualToolActivation> active =
                manualToolActivationRepository.findByCaseFileIdAndDeactivatedAtIsNull(mainCf.getId());
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getToolId()).isEqualTo("F-DT-10-rupture-conv-validity");
        assertThat(active.get(0).getWorkspaceId()).isEqualTo(mainCf.getWorkspace().getId());
    }

    @Test
    void POST_double_meme_outil_retourne_409() throws Exception {
        String body = "{\"toolId\": \"F-DT-10-rupture-conv-validity\"}";

        mockMvc.perform(post(url(mainCf.getId().toString()))
                        .with(authentication(authMain))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post(url(mainCf.getId().toString()))
                        .with(authentication(authMain))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void POST_apres_visibility_l_outil_migre_de_catalog_vers_contextual() throws Exception {
        // 1. Avant activation : F-DT-10 dans catalog (Travail FR sans analyse).
        mockMvc.perform(get("/api/v1/case-files/" + mainCf.getId() + "/decision-tools-visibility")
                        .with(authentication(authMain)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.catalog", org.hamcrest.Matchers.hasItem("F-DT-10-rupture-conv-validity")))
                .andExpect(jsonPath("$.contextual", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("F-DT-10-rupture-conv-validity"))));

        // 2. Activation manuelle
        mockMvc.perform(post(url(mainCf.getId().toString()))
                        .with(authentication(authMain))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toolId\": \"F-DT-10-rupture-conv-validity\"}"))
                .andExpect(status().isOk());

        // 3. Visibility post-activation : F-DT-10 dans contextual, hors catalog
        mockMvc.perform(get("/api/v1/case-files/" + mainCf.getId() + "/decision-tools-visibility")
                        .with(authentication(authMain)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextual", org.hamcrest.Matchers.hasItem("F-DT-10-rupture-conv-validity")))
                .andExpect(jsonPath("$.catalog", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("F-DT-10-rupture-conv-validity"))));
    }

    @Test
    void DELETE_desactive_un_outil_actif_retourne_204_et_redescend_dans_catalog() throws Exception {
        mockMvc.perform(post(url(mainCf.getId().toString()))
                        .with(authentication(authMain))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toolId\": \"F-DT-10-rupture-conv-validity\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete(url(mainCf.getId().toString()) + "/F-DT-10-rupture-conv-validity")
                        .with(authentication(authMain)))
                .andExpect(status().isNoContent());

        List<ManualToolActivation> active =
                manualToolActivationRepository.findByCaseFileIdAndDeactivatedAtIsNull(mainCf.getId());
        assertThat(active).isEmpty();

        // L'outil redescend dans catalog
        mockMvc.perform(get("/api/v1/case-files/" + mainCf.getId() + "/decision-tools-visibility")
                        .with(authentication(authMain)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.catalog", org.hamcrest.Matchers.hasItem("F-DT-10-rupture-conv-validity")));
    }

    @Test
    void DELETE_idempotent_sur_outil_jamais_active_retourne_204() throws Exception {
        mockMvc.perform(delete(url(mainCf.getId().toString()) + "/F-DT-10-rupture-conv-validity")
                        .with(authentication(authMain)))
                .andExpect(status().isNoContent());
    }

    // ──────────────────────────── Erreurs ───────────────────────────────────

    @Test
    void POST_body_vide_retourne_400() throws Exception {
        mockMvc.perform(post(url(mainCf.getId().toString()))
                        .with(authentication(authMain))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toolId\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_case_file_autre_workspace_retourne_404() throws Exception {
        // authMain tente d'activer sur le case_file du workspace ws2.
        mockMvc.perform(post(url(otherWsCf.getId().toString()))
                        .with(authentication(authMain))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toolId\": \"F-DT-10-rupture-conv-validity\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_case_file_inexistant_retourne_404() throws Exception {
        mockMvc.perform(post(url("00000000-0000-0000-0000-000000000000"))
                        .with(authentication(authMain))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toolId\": \"F-DT-10-rupture-conv-validity\"}"))
                .andExpect(status().isNotFound());
    }

    // ──────────────────────────── Helpers ───────────────────────────────────

    private static String url(String caseFileId) {
        return "/api/v1/case-files/" + caseFileId + "/decision-tools-visibility/manual-activations";
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
