package fr.ailegalcase.backlog;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class BacklogControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private BacklogSyncService syncService;
    @Autowired private BacklogFeatureRepository featureRepository;
    @Autowired private BacklogSubfeatureRepository subfeatureRepository;
    @Autowired private BacklogMarketingTaskRepository marketingRepository;
    @Autowired private BacklogSyncRunRepository runRepository;

    @BeforeEach
    void setUp() {
        runRepository.deleteAll();
        subfeatureRepository.deleteAll();
        featureRepository.deleteAll();
        marketingRepository.deleteAll();
    }

    @Test
    void postSync_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/super-admin/backlog/sync")).andExpect(status().isUnauthorized());
    }

    @Test
    void postSync_withRegularUser_returns403() throws Exception {
        registerUser("backlog-regular-sub", "regular@x.test", false);
        mockMvc.perform(post("/api/v1/super-admin/backlog/sync")
                        .with(authentication(buildAuth("backlog-regular-sub", "regular@x.test"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void postSync_withSuperAdmin_returns200() throws Exception {
        registerUser("backlog-superadmin-sub", "sa@x.test", true);
        mockMvc.perform(post("/api/v1/super-admin/backlog/sync")
                        .with(authentication(buildAuth("backlog-superadmin-sub", "sa@x.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.featuresCount").exists());
    }

    @Test
    void getFeatures_withSuperAdmin_returnsPagedList() throws Exception {
        registerUser("backlog-sa-list-sub", "sa-list@x.test", true);
        syncService.sync(SyncTrigger.MANUAL);

        mockMvc.perform(get("/api/v1/super-admin/backlog/features")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(buildAuth("backlog-sa-list-sub", "sa-list@x.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getFeatures_filteredByStatus() throws Exception {
        registerUser("backlog-sa-filter-sub", "sa-f@x.test", true);
        syncService.sync(SyncTrigger.MANUAL);

        mockMvc.perform(get("/api/v1/super-admin/backlog/features?status=DONE")
                        .with(authentication(buildAuth("backlog-sa-filter-sub", "sa-f@x.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("F-DT-08"));
    }

    @Test
    void getFeatureDetail_returnsSubfeatures() throws Exception {
        registerUser("backlog-sa-detail-sub", "sa-d@x.test", true);
        syncService.sync(SyncTrigger.MANUAL);

        mockMvc.perform(get("/api/v1/super-admin/backlog/features/F-DT-08")
                        .with(authentication(buildAuth("backlog-sa-detail-sub", "sa-d@x.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("F-DT-08"))
                .andExpect(jsonPath("$.subfeatures").isArray());
    }

    @Test
    void getFeatureDetail_unknownCode_returns404() throws Exception {
        registerUser("backlog-sa-404-sub", "sa-404@x.test", true);

        mockMvc.perform(get("/api/v1/super-admin/backlog/features/F-INEXISTANT")
                        .with(authentication(buildAuth("backlog-sa-404-sub", "sa-404@x.test"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMarketingTasks_returnsList() throws Exception {
        registerUser("backlog-sa-mktg-sub", "sa-m@x.test", true);
        syncService.sync(SyncTrigger.MANUAL);

        mockMvc.perform(get("/api/v1/super-admin/backlog/marketing-tasks")
                        .with(authentication(buildAuth("backlog-sa-mktg-sub", "sa-m@x.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].code").value("M-01"));
    }

    @Test
    void getSyncRuns_returnsHistory() throws Exception {
        registerUser("backlog-sa-runs-sub", "sa-r@x.test", true);
        syncService.sync(SyncTrigger.MANUAL);

        mockMvc.perform(get("/api/v1/super-admin/backlog/sync-runs?limit=5")
                        .with(authentication(buildAuth("backlog-sa-runs-sub", "sa-r@x.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].triggeredBy").value("MANUAL"));
    }

    @Test
    void getFreshness_returnsStatus() throws Exception {
        registerUser("backlog-sa-fresh-sub", "sa-fr@x.test", true);
        syncService.sync(SyncTrigger.MANUAL);

        mockMvc.perform(get("/api/v1/super-admin/backlog/freshness")
                        .with(authentication(buildAuth("backlog-sa-fresh-sub", "sa-fr@x.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void allEndpoints_rejectRegularUserWith403() throws Exception {
        registerUser("backlog-regular-all-sub", "regular-all@x.test", false);
        OAuth2AuthenticationToken auth = buildAuth("backlog-regular-all-sub", "regular-all@x.test");

        mockMvc.perform(get("/api/v1/super-admin/backlog/features").with(authentication(auth)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/super-admin/backlog/features/F-DT-08").with(authentication(auth)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/super-admin/backlog/marketing-tasks").with(authentication(auth)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/super-admin/backlog/sync-runs").with(authentication(auth)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/super-admin/backlog/freshness").with(authentication(auth)))
                .andExpect(status().isForbidden());
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

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        BacklogProperties testBacklogProperties() throws IOException {
            Path tempDir = Files.createTempDirectory("backlog-controller-it");
            tempDir.toFile().deleteOnExit();
            Path techFile = tempDir.resolve("PRODUCT_SPEC.md");
            Path mktgFile = tempDir.resolve("MARKETING_BACKLOG.md");
            Files.writeString(techFile, """
                    ### Features métier — Droit du travail
                    | ID | Feature | Cible | Notes |
                    |----|---------|-------|-------|
                    | F-DT-08 | Validité licenciement | V1 — **Terminée** | 🟢 SF-DT-08-01 mergée. |
                    | F-DT-09 | Comparateur indemnités | V1 — **En cours** | 🟡 |
                    """);
            Files.writeString(mktgFile, """
                    ## Site
                    | ID | Action | Priorité | Statut | Notes |
                    |----|--------|----------|--------|-------|
                    | M-01 | Landing page | Haute | `Terminé` | Done. |
                    """);
            return new BacklogProperties(techFile.toString(), mktgFile.toString(), 10);
        }
    }
}
