package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-180 SF-180-01 — Tests d'intégration des endpoints d'audit dashboard
 * et de l'instrumentation des crashes via {@link DashboardTileCrashRecorder}.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class DashboardAuditIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private DashboardTileCrashRepository crashRepository;
    @Autowired private DashboardAuditRunRepository runRepository;
    @Autowired private DashboardTileCrashRecorder crashRecorder;
    @Autowired private DashboardAuditService auditService;

    @BeforeEach
    void setUp() {
        runRepository.deleteAll();
        crashRepository.deleteAll();
    }

    @Test
    void getLatest_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/super-admin/dashboard-audit/latest"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getLatest_withRegularUser_returns403() throws Exception {
        registerUser("audit-regular-sub", "audit-regular@x.test", false);
        mockMvc.perform(get("/api/v1/super-admin/dashboard-audit/latest")
                        .with(authentication(buildAuth("audit-regular-sub", "audit-regular@x.test"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLatest_withSuperAdmin_returns200WithReportStructure() throws Exception {
        registerUser("audit-sa-latest-sub", "audit-sa-latest@x.test", true);
        mockMvc.perform(get("/api/v1/super-admin/dashboard-audit/latest")
                        .with(authentication(buildAuth("audit-sa-latest-sub", "audit-sa-latest@x.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ranAt").exists())
                .andExpect(jsonPath("$.crashedMappers").isArray())
                .andExpect(jsonPath("$.dormantTiles").isArray())
                .andExpect(jsonPath("$.activeTiles").isArray());
    }

    @Test
    void postRun_withRegularUser_returns403() throws Exception {
        registerUser("audit-run-regular-sub", "audit-run-reg@x.test", false);
        mockMvc.perform(post("/api/v1/super-admin/dashboard-audit/run")
                        .with(authentication(buildAuth("audit-run-regular-sub", "audit-run-reg@x.test"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void postRun_withSuperAdmin_persistsNewRun() throws Exception {
        registerUser("audit-run-sa-sub", "audit-run-sa@x.test", true);
        long before = runRepository.count();

        mockMvc.perform(post("/api/v1/super-admin/dashboard-audit/run")
                        .with(authentication(buildAuth("audit-run-sa-sub", "audit-run-sa@x.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ranAt").exists());

        assertThat(runRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void crashRecorder_persistsCrashRow() {
        crashRecorder.record("F-DT-08-licenciement-validity", UUID.randomUUID(),
                new IllegalStateException("boom in mapper"));

        List<DashboardTileCrash> crashes = crashRepository.findAll();
        assertThat(crashes).hasSize(1);
        assertThat(crashes.get(0).getToolId()).isEqualTo("F-DT-08-licenciement-validity");
        assertThat(crashes.get(0).getExceptionClass())
                .isEqualTo("java.lang.IllegalStateException");
        assertThat(crashes.get(0).getExceptionMessage()).isEqualTo("boom in mapper");
        assertThat(crashes.get(0).getOccurredAt()).isNotNull();
    }

    @Test
    void runAudit_aggregatesPersistedCrashByToolId() {
        crashRecorder.record("F-IM-05-arbre-decisionnel-titre", null,
                new RuntimeException("npe 1"));
        crashRecorder.record("F-IM-05-arbre-decisionnel-titre", null,
                new RuntimeException("npe 2"));

        var report = auditService.runAudit();

        assertThat(report.crashedMappers())
                .anySatisfy(m -> {
                    assertThat(m.toolId()).isEqualTo("F-IM-05-arbre-decisionnel-titre");
                    assertThat(m.crashCount()).isEqualTo(2);
                });
    }

    @Test
    void crashRecorder_truncatesLongMessage() {
        String huge = "x".repeat(DashboardTileCrash.MAX_MESSAGE_LENGTH + 500);
        crashRecorder.record("F-DT-09-comparateur-indemnites", null,
                new RuntimeException(huge));

        DashboardTileCrash crash = crashRepository.findAll().get(0);
        assertThat(crash.getExceptionMessage())
                .hasSize(DashboardTileCrash.MAX_MESSAGE_LENGTH);
    }

    private void registerUser(String sub, String email, boolean superAdmin) {
        User user = new User();
        user.setEmail(email);
        user.setStatus("ACTIVE");
        user.setSuperAdmin(superAdmin);
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId(sub);
        authAccountRepository.save(account);
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of(
                "sub", sub,
                "email", email,
                "iss", "https://accounts.google.com"
        );
        OidcIdToken idToken = new OidcIdToken("token-value", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
