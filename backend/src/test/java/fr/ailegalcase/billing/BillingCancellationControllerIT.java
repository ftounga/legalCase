package fr.ailegalcase.billing;

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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class BillingCancellationControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;

    // L'appel Stripe réel est remplacé : on contrôle la subscription Stripe renvoyée.
    @MockBean private StripeCustomerService stripeCustomerService;

    private OAuth2AuthenticationToken ownerAuth;
    private OAuth2AuthenticationToken lawyerAuth;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User owner = newUser("owner-cancel@example.com");
        newAuthAccount(owner, "google-owner-cancel");
        User lawyer = newUser("lawyer-cancel@example.com");
        newAuthAccount(lawyer, "google-lawyer-cancel");

        workspace = new Workspace();
        workspace.setName("owner-cancel@example.com");
        workspace.setSlug("cancel-slug-" + System.currentTimeMillis());
        workspace.setOwner(owner);
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setPlanCode("SOLO");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        newMember(workspace, owner, "OWNER");
        newMember(workspace, lawyer, "LAWYER");

        ownerAuth = buildGoogleAuth("google-owner-cancel", "owner-cancel@example.com");
        lawyerAuth = buildGoogleAuth("google-lawyer-cancel", "lawyer-cancel@example.com");
    }

    private Subscription seedSubscription(String planCode, String stripeSubId, boolean cancelAtPeriodEnd) {
        Subscription sub = new Subscription();
        sub.setWorkspaceId(workspace.getId());
        sub.setPlanCode(planCode);
        sub.setStatus("ACTIVE");
        sub.setStartedAt(Instant.now());
        sub.setStripeSubscriptionId(stripeSubId);
        sub.setCancelAtPeriodEnd(cancelAtPeriodEnd);
        return subscriptionRepository.save(sub);
    }

    private com.stripe.model.Subscription stripeSubWithPeriodEnd(long epochSeconds) {
        com.stripe.model.Subscription stripeSub = new com.stripe.model.Subscription();
        stripeSub.setCurrentPeriodEnd(epochSeconds);
        return stripeSub;
    }

    // I-01 : POST /cancel en tant qu'OWNER d'un workspace payant → 200, cancelAtPeriodEnd=true
    @Test
    void cancel_owner_paidWorkspace_returns200() throws Exception {
        seedSubscription("SOLO", "sub_123", false);
        when(stripeCustomerService.scheduleCancellation(anyString()))
                .thenReturn(stripeSubWithPeriodEnd(1_900_000_000L));

        mockMvc.perform(post("/api/v1/billing/cancel").with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("SOLO"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.cancelAtPeriodEnd").value(true))
                .andExpect(jsonPath("$.currentPeriodEnd").exists());
    }

    // I-02 : POST /cancel par un membre non-OWNER → 403
    @Test
    void cancel_nonOwner_returns403() throws Exception {
        seedSubscription("SOLO", "sub_123", false);

        mockMvc.perform(post("/api/v1/billing/cancel").with(authentication(lawyerAuth)))
                .andExpect(status().isForbidden());
    }

    // I-03 : POST /cancel sur un workspace FREE → 409
    @Test
    void cancel_freeWorkspace_returns409() throws Exception {
        seedSubscription("FREE", null, false);

        mockMvc.perform(post("/api/v1/billing/cancel").with(authentication(ownerAuth)))
                .andExpect(status().isConflict());
    }

    // I-04 : POST /resume après une résiliation programmée → 200, puis 409 au second appel
    @Test
    void resume_afterCancellation_returns200ThenConflict() throws Exception {
        seedSubscription("SOLO", "sub_123", true);
        when(stripeCustomerService.resumeSubscription(anyString()))
                .thenReturn(stripeSubWithPeriodEnd(1_900_000_000L));

        mockMvc.perform(post("/api/v1/billing/resume").with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelAtPeriodEnd").value(false));

        mockMvc.perform(post("/api/v1/billing/resume").with(authentication(ownerAuth)))
                .andExpect(status().isConflict());
    }

    // I-05 : GET /subscription → 200 avec les 4 champs du contrat
    @Test
    void getSubscription_returns200WithContractFields() throws Exception {
        seedSubscription("SOLO", "sub_123", true);

        mockMvc.perform(get("/api/v1/billing/subscription").with(authentication(lawyerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("SOLO"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.cancelAtPeriodEnd").value(true))
                .andExpect(jsonPath("$.currentPeriodEnd").hasJsonPath());
    }

    // I-06 : POST /cancel sans authentification → 401
    @Test
    void cancel_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/billing/cancel"))
                .andExpect(status().isUnauthorized());
    }

    private User newUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setStatus("ACTIVE");
        return userRepository.save(u);
    }

    private void newAuthAccount(User user, String sub) {
        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId(sub);
        authAccountRepository.save(account);
    }

    private void newMember(Workspace ws, User user, String role) {
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);
        member.setUser(user);
        member.setMemberRole(role);
        member.setPrimary(true);
        workspaceMemberRepository.save(member);
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
