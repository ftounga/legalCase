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
        "spring.security.oauth2.client.registration.microsoft.client-id=test-microsoft-id",
        "spring.security.oauth2.client.registration.microsoft.client-secret=test-microsoft-secret"
})
@AutoConfigureMockMvc
class UsageEventControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private UsageEventRepository usageEventRepository;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;
    private User user;

    @BeforeEach
    void setUp() {
        usageEventRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("usage-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-usage-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("usage-test@example.com");
        workspace.setSlug("usage-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        workspaceMemberRepository.save(member);

        caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFile.setTitle("Dossier Test Usage");
        caseFile.setLegalDomain("EMPLOYMENT_LAW");
        caseFile.setStatus("OPEN");
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-usage-sub", "usage-test@example.com");
    }

    // I-01 : GET usage avec events → 200 + données correctes
    @Test
    void list_withEvents_returns200AndEvents() throws Exception {
        UsageEvent event = new UsageEvent();
        event.setCaseFileId(caseFile.getId());
        event.setUserId(user.getId());
        event.setEventType(JobType.CASE_ANALYSIS);
        event.setTokensInput(500);
        event.setTokensOutput(200);
        event.setEstimatedCost(new BigDecimal("0.004500"));
        usageEventRepository.save(event);

        mockMvc.perform(get("/api/v1/case-files/{id}/usage", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventType").value("CASE_ANALYSIS"))
                .andExpect(jsonPath("$[0].tokensInput").value(500))
                .andExpect(jsonPath("$[0].tokensOutput").value(200));
    }

    // I-02 : GET usage sans events → 200 liste vide
    @Test
    void list_noEvents_returns200EmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/usage", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // I-03 : GET dossier inconnu → 404
    @Test
    void list_unknownCaseFile_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/usage", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-04 : GET dossier d'un autre workspace → 404 (isolation workspace)
    @Test
    void list_otherWorkspaceCaseFile_returns404() throws Exception {
        User otherUser = new User();
        otherUser.setEmail("other-usage@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-other-usage-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-usage@example.com");
        otherWorkspace.setSlug("other-usage-slug-" + System.currentTimeMillis());
        otherWorkspace.setOwner(otherUser);
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        workspaceRepository.save(otherWorkspace);

        WorkspaceMember otherMember = new WorkspaceMember();
        otherMember.setWorkspace(otherWorkspace);
        otherMember.setUser(otherUser);
        otherMember.setMemberRole("OWNER");
        workspaceMemberRepository.save(otherMember);

        CaseFile otherCaseFile = new CaseFile();
        otherCaseFile.setWorkspace(otherWorkspace);
        otherCaseFile.setCreatedBy(otherUser);
        otherCaseFile.setTitle("Dossier Autre Workspace");
        otherCaseFile.setLegalDomain("EMPLOYMENT_LAW");
        otherCaseFile.setStatus("OPEN");
        caseFileRepository.save(otherCaseFile);

        mockMvc.perform(get("/api/v1/case-files/{id}/usage", otherCaseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-05 : GET sans auth → 401
    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/usage", caseFile.getId()))
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
