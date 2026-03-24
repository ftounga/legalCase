package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.workspace.*;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class AdminUsageControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private UsageEventRepository usageEventRepository;

    private User ownerUser;
    private Workspace workspace;
    private CaseFile caseFile;
    private OAuth2AuthenticationToken ownerAuth;

    @BeforeEach
    void setUp() {
        usageEventRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        ownerUser = new User();
        ownerUser.setEmail("admin-test@example.com");
        ownerUser.setStatus("ACTIVE");
        userRepository.save(ownerUser);

        AuthAccount account = new AuthAccount();
        account.setUser(ownerUser);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-admin-sub");
        authAccountRepository.save(account);

        workspace = new Workspace();
        workspace.setName("admin-test@example.com");
        workspace.setSlug("admin-slug-" + System.currentTimeMillis());
        workspace.setOwner(ownerUser);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
       workspace.setLegalDomain("DROIT_DU_TRAVAIL");

        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(ownerUser);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(ownerUser);
        caseFile.setTitle("Dossier Admin Test");
        caseFile.setStatus("OPEN");
        caseFileRepository.save(caseFile);

        ownerAuth = buildAuth("google-admin-sub", "admin-test@example.com");
    }

    // I-01 : GET /admin/usage OWNER avec events → 200 + totaux corrects
    @Test
    void getSummary_ownerWithEvents_returns200AndSummary() throws Exception {
        UsageEvent event = new UsageEvent();
        event.setCaseFileId(caseFile.getId());
        event.setUserId(ownerUser.getId());
        event.setEventType(JobType.CASE_ANALYSIS);
        event.setTokensInput(500);
        event.setTokensOutput(200);
        event.setEstimatedCost(new BigDecimal("0.005000"));
        usageEventRepository.save(event);

        mockMvc.perform(get("/api/v1/admin/usage").with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTokensInput").value(500))
                .andExpect(jsonPath("$.totalTokensOutput").value(200))
                .andExpect(jsonPath("$.byUser.length()").value(1))
                .andExpect(jsonPath("$.byCaseFile.length()").value(1))
                .andExpect(jsonPath("$.byCaseFile[0].caseFileTitle").value("Dossier Admin Test"));
    }

    // I-02 : GET /admin/usage ADMIN → 200
    @Test
    void getSummary_admin_returns200() throws Exception {
        User adminUser = new User();
        adminUser.setEmail("admin2@example.com");
        adminUser.setStatus("ACTIVE");
        userRepository.save(adminUser);

        AuthAccount adminAccount = new AuthAccount();
        adminAccount.setUser(adminUser);
        adminAccount.setProvider("GOOGLE");
        adminAccount.setProviderUserId("google-admin2-sub");
        authAccountRepository.save(adminAccount);

        WorkspaceMember adminMember = new WorkspaceMember();
        adminMember.setWorkspace(workspace);
        adminMember.setUser(adminUser);
        adminMember.setMemberRole("ADMIN");
        adminMember.setPrimary(true);
        workspaceMemberRepository.save(adminMember);

        OAuth2AuthenticationToken adminAuth = buildAuth("google-admin2-sub", "admin2@example.com");

        mockMvc.perform(get("/api/v1/admin/usage").with(authentication(adminAuth)))
                .andExpect(status().isOk());
    }

    // I-03 : GET /admin/usage LAWYER → 403
    @Test
    void getSummary_lawyer_returns403() throws Exception {
        User lawyerUser = new User();
        lawyerUser.setEmail("lawyer@example.com");
        lawyerUser.setStatus("ACTIVE");
        userRepository.save(lawyerUser);

        AuthAccount lawyerAccount = new AuthAccount();
        lawyerAccount.setUser(lawyerUser);
        lawyerAccount.setProvider("GOOGLE");
        lawyerAccount.setProviderUserId("google-lawyer-sub");
        authAccountRepository.save(lawyerAccount);

        WorkspaceMember lawyerMember = new WorkspaceMember();
        lawyerMember.setWorkspace(workspace);
        lawyerMember.setUser(lawyerUser);
        lawyerMember.setMemberRole("LAWYER");
        lawyerMember.setPrimary(true);
        workspaceMemberRepository.save(lawyerMember);

        OAuth2AuthenticationToken lawyerAuth = buildAuth("google-lawyer-sub", "lawyer@example.com");

        mockMvc.perform(get("/api/v1/admin/usage").with(authentication(lawyerAuth)))
                .andExpect(status().isForbidden());
    }

    // I-04 : GET /admin/usage sans auth → 401
    @Test
    void getSummary_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/usage"))
                .andExpect(status().isUnauthorized());
    }

    // I-05 : GET /admin/usage sans events → 200 avec listes vides
    @Test
    void getSummary_noEvents_returns200WithEmptyLists() throws Exception {
        mockMvc.perform(get("/api/v1/admin/usage").with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTokensInput").value(0))
                .andExpect(jsonPath("$.byUser.length()").value(0))
                .andExpect(jsonPath("$.byCaseFile.length()").value(0));
    }

    // I-06 : isolation workspace — OWNER d'un autre workspace ne voit pas ces données
    @Test
    void getSummary_otherWorkspaceOwner_returnsOwnEmptyData() throws Exception {
        // Créer un event pour le workspace de ownerUser
        UsageEvent event = new UsageEvent();
        event.setCaseFileId(caseFile.getId());
        event.setUserId(ownerUser.getId());
        event.setEventType(JobType.CASE_ANALYSIS);
        event.setTokensInput(999);
        event.setTokensOutput(999);
        event.setEstimatedCost(new BigDecimal("0.999000"));
        usageEventRepository.save(event);

        // Créer un autre workspace + owner
        User otherUser = new User();
        otherUser.setEmail("other-admin@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-other-admin-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-admin@example.com");
        otherWorkspace.setSlug("other-admin-slug-" + System.currentTimeMillis());
        otherWorkspace.setOwner(otherUser);
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        workspaceRepository.save(otherWorkspace);

        WorkspaceMember otherMember = new WorkspaceMember();
        otherMember.setWorkspace(otherWorkspace);
        otherMember.setUser(otherUser);
        otherMember.setMemberRole("OWNER");
        otherMember.setPrimary(true);
        workspaceMemberRepository.save(otherMember);

        OAuth2AuthenticationToken otherAuth = buildAuth("google-other-admin-sub", "other-admin@example.com");

        // L'autre owner voit ses propres données (vides), pas les 999 tokens du workspace 1
        mockMvc.perform(get("/api/v1/admin/usage").with(authentication(otherAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTokensInput").value(0));
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of(
                "sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
