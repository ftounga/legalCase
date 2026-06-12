package fr.ailegalcase.casefile.strategy;

import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileDashboardService;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.DashboardTile;
import fr.ailegalcase.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

/**
 * F-286 / SF-286-01 — IT des endpoints de la stratégie de dossier.
 * {@link AnthropicService} et {@link CaseFileDashboardService} sont mockés pour piloter
 * les branches READY / EMPTY_INPUT sans appel réel ni données d'outils en base.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class CaseStrategyControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private CaseStrategyRepository strategyRepository;

    @MockBean private AnthropicService anthropicService;
    @MockBean private CaseFileDashboardService dashboardService;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;
    private Workspace workspace;
    private User user;

    @BeforeEach
    void setUp() {
        strategyRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("strategy-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-strategy-sub");
        authAccountRepository.save(account);

        workspace = new Workspace();
        workspace.setName("STRATEGY-TEST");
        workspace.setSlug("strategy-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        caseFile = new CaseFile();
        caseFile.setTitle("Dossier stratégie");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-strategy-sub", "strategy-test@example.com");
    }

    // I-01 : aucune stratégie générée → EMPTY
    @Test
    void get_beforeGeneration_returnsEmpty() throws Exception {
        when(dashboardService.assembleDecisionToolTiles(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/case-files/{id}/strategy", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EMPTY"))
                .andExpect(jsonPath("$.content").doesNotExist());
        verifyNoInteractions(anthropicService);
    }

    // I-02 : POST avec 0 outil calculé → EMPTY_INPUT, AUCUN appel Anthropic
    @Test
    void post_noCalculatedTool_returnsEmptyInput_andSkipsLlm() throws Exception {
        when(dashboardService.assembleDecisionToolTiles(any())).thenReturn(List.of());
        mockMvc.perform(post("/api/v1/case-files/{id}/strategy", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EMPTY_INPUT"))
                .andExpect(jsonPath("$.toolsConsidered").value(0));
        verifyNoInteractions(anthropicService);
    }

    // I-03 : POST avec ≥1 outil calculé → READY, content rempli, persisté
    @Test
    void post_withCalculatedTool_returnsReady_andPersists() throws Exception {
        when(dashboardService.assembleDecisionToolTiles(any())).thenReturn(List.of(
                new DashboardTile("F-DT-08", "VALIDITE", "Validité du licenciement",
                        "Sans cause réelle et sérieuse", "2/7", "ALERT")));
        when(anthropicService.analyze(any(), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult("## Voie procédurale\nFond.", "claude-sonnet", 100, 50));

        mockMvc.perform(post("/api/v1/case-files/{id}/strategy", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.content").value("## Voie procédurale\nFond."))
                .andExpect(jsonPath("$.toolsConsidered").value(1))
                .andExpect(jsonPath("$.modelUsed").value("claude-sonnet"));

        // persisté + relisible
        mockMvc.perform(get("/api/v1/case-files/{id}/strategy", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.content").value("## Voie procédurale\nFond."));
    }

    // I-04 : régénération → upsert (1 seule ligne)
    @Test
    void post_twice_upsertsSingleRow() throws Exception {
        when(dashboardService.assembleDecisionToolTiles(any())).thenReturn(List.of(
                new DashboardTile("F-DT-08", "VALIDITE", "Validité", "OK", null, "OK")));
        when(anthropicService.analyze(any(), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult("v1", "m", 10, 5))
                .thenReturn(new AnthropicResult("v2", "m", 10, 5));

        mockMvc.perform(post("/api/v1/case-files/{id}/strategy", caseFile.getId()).with(authentication(auth)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/case-files/{id}/strategy", caseFile.getId()).with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("v2"));

        org.assertj.core.api.Assertions.assertThat(strategyRepository.count()).isEqualTo(1);
    }

    // I-05 : dossier d'un autre workspace → 404
    @Test
    void get_caseFileFromOtherWorkspace_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/strategy", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-06 : sans auth → 401
    @Test
    void get_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/strategy", caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    private OAuth2AuthenticationToken buildGoogleAuth(String sub, String email) {
        Map<String, Object> claims = Map.of(
                "sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
