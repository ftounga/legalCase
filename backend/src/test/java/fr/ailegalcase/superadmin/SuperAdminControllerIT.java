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
import java.util.UUID;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    // I-08 : DELETE /api/v1/super-admin/workspaces/{id} → 204, workspace supprimé
    @Test
    void deleteWorkspace_withSuperAdmin_returns204AndDeletesWorkspace() throws Exception {
        User superAdmin = new User();
        superAdmin.setEmail("superadmin-del@example.com");
        superAdmin.setStatus("ACTIVE");
        superAdmin.setSuperAdmin(true);
        userRepository.save(superAdmin);

        AuthAccount account = new AuthAccount();
        account.setUser(superAdmin);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-superadmin-del-sub");
        authAccountRepository.save(account);

        User owner = new User();
        owner.setEmail("owner-del@example.com");
        owner.setStatus("ACTIVE");
        userRepository.save(owner);

        Workspace ws = new Workspace();
        ws.setName("Cabinet À Supprimer");
        ws.setSlug("cabinet-a-supprimer-" + System.currentTimeMillis());
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

        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(ws);
        caseFile.setCreatedBy(owner);
        caseFile.setTitle("Dossier À Supprimer");
        caseFile.setLegalDomain("EMPLOYMENT_LAW");
        caseFile.setStatus("ACTIVE");
        caseFileRepository.save(caseFile);

        UUID wsId = ws.getId();
        UUID cfId = caseFile.getId();

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-superadmin-del-sub", "superadmin-del@example.com");

        mockMvc.perform(delete("/api/v1/super-admin/workspaces/" + wsId)
                        .with(authentication(auth)))
                .andExpect(status().isNoContent());

        assertThat(workspaceRepository.findById(wsId)).isEmpty();
        assertThat(caseFileRepository.findById(cfId)).isEmpty();
    }

    // I-09 : DELETE /api/v1/super-admin/workspaces/{uuid-inexistant} → 404
    @Test
    void deleteWorkspace_unknownId_returns404() throws Exception {
        User superAdmin = new User();
        superAdmin.setEmail("superadmin-404@example.com");
        superAdmin.setStatus("ACTIVE");
        superAdmin.setSuperAdmin(true);
        userRepository.save(superAdmin);

        AuthAccount account = new AuthAccount();
        account.setUser(superAdmin);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-superadmin-404-sub");
        authAccountRepository.save(account);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-superadmin-404-sub", "superadmin-404@example.com");

        mockMvc.perform(delete("/api/v1/super-admin/workspaces/" + UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-10 : DELETE /api/v1/super-admin/workspaces/{id} avec non super-admin → 403
    @Test
    void deleteWorkspace_withoutSuperAdmin_returns403() throws Exception {
        User regular = new User();
        regular.setEmail("regular-del@example.com");
        regular.setStatus("ACTIVE");
        regular.setSuperAdmin(false);
        userRepository.save(regular);

        AuthAccount account = new AuthAccount();
        account.setUser(regular);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-regular-del-sub");
        authAccountRepository.save(account);

        User owner = new User();
        owner.setEmail("owner-del2@example.com");
        owner.setStatus("ACTIVE");
        userRepository.save(owner);

        Workspace ws = new Workspace();
        ws.setName("Cabinet Non Supprimable");
        ws.setSlug("cabinet-non-supprimable-" + System.currentTimeMillis());
        ws.setOwner(owner);
        ws.setPlanCode("FREE");
        ws.setStatus("ACTIVE");
        workspaceRepository.save(ws);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-regular-del-sub", "regular-del@example.com");

        mockMvc.perform(delete("/api/v1/super-admin/workspaces/" + ws.getId())
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());

        assertThat(workspaceRepository.findById(ws.getId())).isPresent();
    }

    // I-11 : GET /api/v1/super-admin/users avec super-admin → 200, liste des users
    @Test
    void listUsers_withSuperAdmin_returns200() throws Exception {
        User superAdmin = new User();
        superAdmin.setEmail("superadmin-listusers@example.com");
        superAdmin.setStatus("ACTIVE");
        superAdmin.setSuperAdmin(true);
        userRepository.save(superAdmin);

        AuthAccount account = new AuthAccount();
        account.setUser(superAdmin);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-superadmin-listusers-sub");
        authAccountRepository.save(account);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-superadmin-listusers-sub", "superadmin-listusers@example.com");

        mockMvc.perform(get("/api/v1/super-admin/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // I-12 : GET /api/v1/super-admin/users avec non super-admin → 403
    @Test
    void listUsers_withoutSuperAdmin_returns403() throws Exception {
        User regular = new User();
        regular.setEmail("regular-listusers@example.com");
        regular.setStatus("ACTIVE");
        regular.setSuperAdmin(false);
        userRepository.save(regular);

        AuthAccount account = new AuthAccount();
        account.setUser(regular);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-regular-listusers-sub");
        authAccountRepository.save(account);

        mockMvc.perform(get("/api/v1/super-admin/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(buildGoogleAuth("google-regular-listusers-sub", "regular-listusers@example.com"))))
                .andExpect(status().isForbidden());
    }

    // I-13 : DELETE /api/v1/super-admin/users/{id} → 204, user + auth_accounts supprimés
    @Test
    void deleteUser_withSuperAdmin_returns204AndDeletesUser() throws Exception {
        User superAdmin = new User();
        superAdmin.setEmail("superadmin-delusr@example.com");
        superAdmin.setStatus("ACTIVE");
        superAdmin.setSuperAdmin(true);
        userRepository.save(superAdmin);

        AuthAccount saAccount = new AuthAccount();
        saAccount.setUser(superAdmin);
        saAccount.setProvider("GOOGLE");
        saAccount.setProviderUserId("google-superadmin-delusr-sub");
        authAccountRepository.save(saAccount);

        User target = new User();
        target.setEmail("target-delusr@example.com");
        target.setStatus("ACTIVE");
        target.setSuperAdmin(false);
        userRepository.save(target);

        AuthAccount targetAccount = new AuthAccount();
        targetAccount.setUser(target);
        targetAccount.setProvider("GOOGLE");
        targetAccount.setProviderUserId("google-target-delusr-sub");
        authAccountRepository.save(targetAccount);

        Workspace ws = new Workspace();
        ws.setName("Cabinet Target User");
        ws.setSlug("cabinet-target-user-" + System.currentTimeMillis());
        ws.setOwner(target);
        ws.setPlanCode("FREE");
        ws.setStatus("ACTIVE");
        workspaceRepository.save(ws);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);
        member.setUser(target);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        UUID targetId = target.getId();
        UUID wsId = ws.getId();

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-superadmin-delusr-sub", "superadmin-delusr@example.com");

        mockMvc.perform(delete("/api/v1/super-admin/users/" + targetId)
                        .with(authentication(auth)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(targetId)).isEmpty();
        assertThat(workspaceRepository.findById(wsId)).isEmpty();
    }

    // I-12 : DELETE /api/v1/super-admin/users/{uuid-inexistant} → 404
    @Test
    void deleteUser_unknownId_returns404() throws Exception {
        User superAdmin = new User();
        superAdmin.setEmail("superadmin-delusr404@example.com");
        superAdmin.setStatus("ACTIVE");
        superAdmin.setSuperAdmin(true);
        userRepository.save(superAdmin);

        AuthAccount account = new AuthAccount();
        account.setUser(superAdmin);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-superadmin-delusr404-sub");
        authAccountRepository.save(account);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-superadmin-delusr404-sub", "superadmin-delusr404@example.com");

        mockMvc.perform(delete("/api/v1/super-admin/users/" + UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-13 : DELETE /api/v1/super-admin/users/{id} avec non super-admin → 403
    @Test
    void deleteUser_withoutSuperAdmin_returns403() throws Exception {
        User regular = new User();
        regular.setEmail("regular-delusr@example.com");
        regular.setStatus("ACTIVE");
        regular.setSuperAdmin(false);
        userRepository.save(regular);

        AuthAccount account = new AuthAccount();
        account.setUser(regular);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-regular-delusr-sub");
        authAccountRepository.save(account);

        User target = new User();
        target.setEmail("target-delusr2@example.com");
        target.setStatus("ACTIVE");
        userRepository.save(target);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-regular-delusr-sub", "regular-delusr@example.com");

        mockMvc.perform(delete("/api/v1/super-admin/users/" + target.getId())
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(target.getId())).isPresent();
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
