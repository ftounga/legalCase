package fr.ailegalcase.superadmin;

import fr.ailegalcase.analysis.UsageEvent;
import fr.ailegalcase.analysis.UsageEventRepository;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.microsoft.client-id=test-microsoft-id",
        "spring.security.oauth2.client.registration.microsoft.client-secret=test-microsoft-secret"
})
@AutoConfigureMockMvc
class SuperAdminControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private UsageEventRepository usageEventRepository;

    // I-01 : GET /api/v1/super-admin/workspaces sans auth → 401
    @Test
    void listWorkspaces_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/super-admin/workspaces").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // I-02 : GET /api/v1/super-admin/workspaces avec is_super_admin=false → 403
    @Test
    void listWorkspaces_withoutSuperAdmin_returns403() throws Exception {
        User user = new User();
        user.setEmail("regular-sa@example.com");
        user.setStatus("ACTIVE");
        user.setSuperAdmin(false);
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-regular-sa-sub");
        authAccountRepository.save(account);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-regular-sa-sub", "regular-sa@example.com");

        mockMvc.perform(get("/api/v1/super-admin/workspaces")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    // I-03 : GET /api/v1/super-admin/workspaces avec is_super_admin=true → 200 avec tous les workspaces
    @Test
    void listWorkspaces_withSuperAdmin_returns200WithAllWorkspaces() throws Exception {
        User superAdmin = new User();
        superAdmin.setEmail("superadmin@example.com");
        superAdmin.setStatus("ACTIVE");
        superAdmin.setSuperAdmin(true);
        userRepository.save(superAdmin);

        AuthAccount account = new AuthAccount();
        account.setUser(superAdmin);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-superadmin-sub");
        authAccountRepository.save(account);

        User owner = new User();
        owner.setEmail("owner-sa@example.com");
        owner.setStatus("ACTIVE");
        userRepository.save(owner);

        Workspace ws = new Workspace();
        ws.setName("Cabinet Super Test");
        ws.setSlug("cabinet-super-test-" + System.currentTimeMillis());
        ws.setOwner(owner);
        ws.setPlanCode("STARTER");
        ws.setStatus("ACTIVE");
        workspaceRepository.save(ws);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);
        member.setUser(owner);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-superadmin-sub", "superadmin@example.com");

        mockMvc.perform(get("/api/v1/super-admin/workspaces")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // I-04 : GET /api/v1/super-admin/workspaces → memberCount correct pour chaque workspace
    @Test
    void listWorkspaces_returnsMemberCount() throws Exception {
        User superAdmin = new User();
        superAdmin.setEmail("superadmin2@example.com");
        superAdmin.setStatus("ACTIVE");
        superAdmin.setSuperAdmin(true);
        userRepository.save(superAdmin);

        AuthAccount account = new AuthAccount();
        account.setUser(superAdmin);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-superadmin2-sub");
        authAccountRepository.save(account);

        User owner = new User();
        owner.setEmail("owner2-sa@example.com");
        owner.setStatus("ACTIVE");
        userRepository.save(owner);

        User member2 = new User();
        member2.setEmail("member2-sa@example.com");
        member2.setStatus("ACTIVE");
        userRepository.save(member2);

        Workspace ws = new Workspace();
        ws.setName("Cabinet Deux Membres");
        ws.setSlug("cabinet-deux-membres-" + System.currentTimeMillis());
        ws.setOwner(owner);
        ws.setPlanCode("PRO");
        ws.setStatus("ACTIVE");
        workspaceRepository.save(ws);

        WorkspaceMember m1 = new WorkspaceMember();
        m1.setWorkspace(ws);
        m1.setUser(owner);
        m1.setMemberRole("OWNER");
        m1.setPrimary(true);
        workspaceMemberRepository.save(m1);

        WorkspaceMember m2 = new WorkspaceMember();
        m2.setWorkspace(ws);
        m2.setUser(member2);
        m2.setMemberRole("LAWYER");
        m2.setPrimary(true);
        workspaceMemberRepository.save(m2);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-superadmin2-sub", "superadmin2@example.com");

        mockMvc.perform(get("/api/v1/super-admin/workspaces")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Cabinet Deux Membres')].memberCount").value(2));
    }

    // I-05 : GET /api/v1/super-admin/usage → 403 non super-admin
    @Test
    void getUsage_withoutSuperAdmin_returns403() throws Exception {
        User user = new User();
        user.setEmail("regular-usage@example.com");
        user.setStatus("ACTIVE");
        user.setSuperAdmin(false);
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-regular-usage-sub");
        authAccountRepository.save(account);

        mockMvc.perform(get("/api/v1/super-admin/usage")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(buildGoogleAuth("google-regular-usage-sub", "regular-usage@example.com"))))
                .andExpect(status().isForbidden());
    }

    // I-06 : GET /api/v1/super-admin/usage → 200 avec données agrégées correctes
    @Test
    void getUsage_withSuperAdmin_returnsAggregatedData() throws Exception {
        User superAdmin = new User();
        superAdmin.setEmail("superadmin-usage@example.com");
        superAdmin.setStatus("ACTIVE");
        superAdmin.setSuperAdmin(true);
        userRepository.save(superAdmin);

        AuthAccount account = new AuthAccount();
        account.setUser(superAdmin);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-superadmin-usage-sub");
        authAccountRepository.save(account);

        User owner = new User();
        owner.setEmail("owner-usage@example.com");
        owner.setStatus("ACTIVE");
        userRepository.save(owner);

        Workspace ws = new Workspace();
        ws.setName("Cabinet Usage IT");
        ws.setSlug("cabinet-usage-it-" + System.currentTimeMillis());
        ws.setOwner(owner);
        ws.setPlanCode("PRO");
        ws.setStatus("ACTIVE");
        workspaceRepository.save(ws);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);
        member.setUser(owner);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(ws);
        caseFile.setCreatedBy(owner);
        caseFile.setTitle("Dossier Test Usage");
        caseFile.setLegalDomain("EMPLOYMENT_LAW");
        caseFile.setStatus("ACTIVE");
        caseFileRepository.save(caseFile);

        UsageEvent event = new UsageEvent();
        event.setCaseFileId(caseFile.getId());
        event.setUserId(owner.getId());
        event.setEventType(fr.ailegalcase.analysis.JobType.CHUNK_ANALYSIS);
        event.setTokensInput(1000);
        event.setTokensOutput(500);
        event.setEstimatedCost(new BigDecimal("0.012000"));
        usageEventRepository.save(event);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-superadmin-usage-sub", "superadmin-usage@example.com");

        mockMvc.perform(get("/api/v1/super-admin/usage")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.workspaceName == 'Cabinet Usage IT')].totalTokensInput").value(1000))
                .andExpect(jsonPath("$[?(@.workspaceName == 'Cabinet Usage IT')].totalTokensOutput").value(500));
    }

    // I-07 : workspace sans usage_events → totalCost = 0
    @Test
    void getUsage_workspaceWithNoEvents_returnsTotalCostZero() throws Exception {
        User superAdmin = new User();
        superAdmin.setEmail("superadmin-empty@example.com");
        superAdmin.setStatus("ACTIVE");
        superAdmin.setSuperAdmin(true);
        userRepository.save(superAdmin);

        AuthAccount account = new AuthAccount();
        account.setUser(superAdmin);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-superadmin-empty-sub");
        authAccountRepository.save(account);

        User owner = new User();
        owner.setEmail("owner-empty@example.com");
        owner.setStatus("ACTIVE");
        userRepository.save(owner);

        Workspace ws = new Workspace();
        ws.setName("Cabinet Sans Usage");
        ws.setSlug("cabinet-sans-usage-" + System.currentTimeMillis());
        ws.setOwner(owner);
        ws.setPlanCode("FREE");
        ws.setStatus("ACTIVE");
        workspaceRepository.save(ws);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);
        member.setUser(owner);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-superadmin-empty-sub", "superadmin-empty@example.com");

        mockMvc.perform(get("/api/v1/super-admin/usage")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.workspaceName == 'Cabinet Sans Usage')].totalCost").value(0));
    }

    private OAuth2AuthenticationToken buildGoogleAuth(String sub, String email) {
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
