package fr.ailegalcase.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class PromoCodeRedemptionControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private PromoCodeRepository promoCodeRepository;
    @Autowired private PromoCodeRedemptionRepository redemptionRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PromoCodeService promoCodeService;

    private User owner;
    private Workspace workspace;
    private OAuth2AuthenticationToken ownerAuth;

    @BeforeEach
    void setUp() {
        redemptionRepository.deleteAll();
        promoCodeRepository.deleteAll();
        subscriptionRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        String uid = "-" + UUID.randomUUID();
        owner = newUser("owner" + uid + "@example.com", false);
        newAuthAccount(owner, "google-owner" + uid);
        ownerAuth = buildGoogleAuth("google-owner" + uid, "owner" + uid + "@example.com");

        workspace = newWorkspace("Trial Workspace", "FREE", owner);
        newMember(workspace, owner, "OWNER", true);
        newSubscription(workspace.getId(), "FREE", Instant.now().plus(5, ChronoUnit.DAYS));
    }

    // C6 — Redemption nominale TRIAL_EXTENSION
    @Test
    void redeem_nominal_extendsSubscriptionAndPersistsRedemption() throws Exception {
        seedCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100, true);
        Instant initialExpiresAt = subscriptionRepository.findByWorkspaceId(workspace.getId())
                .orElseThrow().getExpiresAt();

        mockMvc.perform(post("/api/v1/workspaces/" + workspace.getId() + "/billing/promo-codes/redeem")
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ace2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addedDays").value(30))
                .andExpect(jsonPath("$.partnerLabel").value("ACE"))
                .andExpect(jsonPath("$.newExpiresAt").exists());

        Subscription sub = subscriptionRepository.findByWorkspaceId(workspace.getId()).orElseThrow();
        assertThat(sub.getExpiresAt()).isEqualTo(initialExpiresAt.plus(30, ChronoUnit.DAYS));

        List<PromoCodeRedemption> redemptions = redemptionRepository.findAll();
        assertThat(redemptions).hasSize(1);
        PromoCodeRedemption r = redemptions.get(0);
        assertThat(r.getWorkspaceId()).isEqualTo(workspace.getId());
        assertThat(r.getCodeAtRedemption()).isEqualTo("ACE2026");
        assertThat(r.getType()).isEqualTo(PromoCodeType.TRIAL_EXTENSION);
        assertThat(r.getValueAppliedDays()).isEqualTo(30);
        assertThat(r.getAppliedByUserId()).isEqualTo(owner.getId());
    }

    // C7 — Code expiré → 409 PROMO_CODE_EXPIRED
    @Test
    void redeem_expiredCode_returns409Expired() throws Exception {
        PromoCode c = new PromoCode();
        c.setCode("EXPIRED");
        c.setType(PromoCodeType.TRIAL_EXTENSION);
        c.setValueDays(30);
        c.setPartnerLabel("ACE");
        c.setMaxUses(100);
        c.setUsesCount(0);
        c.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        c.setActive(true);
        c.setCreatedAt(Instant.now().minus(10, ChronoUnit.DAYS));
        c.setCreatedByUserId(owner.getId());
        promoCodeRepository.save(c);

        mockMvc.perform(post("/api/v1/workspaces/" + workspace.getId() + "/billing/promo-codes/redeem")
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"EXPIRED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROMO_CODE_EXPIRED"));
    }

    // C8 — Code désactivé → 409 PROMO_CODE_INACTIVE
    @Test
    void redeem_inactiveCode_returns409Inactive() throws Exception {
        seedCode("INACTIVE", PromoCodeType.TRIAL_EXTENSION, 30, 100, false);

        mockMvc.perform(post("/api/v1/workspaces/" + workspace.getId() + "/billing/promo-codes/redeem")
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"INACTIVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROMO_CODE_INACTIVE"));
    }

    // C9 — uses_count == max_uses → 409 PROMO_CODE_EXHAUSTED
    @Test
    void redeem_exhaustedCode_returns409Exhausted() throws Exception {
        PromoCode c = new PromoCode();
        c.setCode("FULL");
        c.setType(PromoCodeType.TRIAL_EXTENSION);
        c.setValueDays(30);
        c.setPartnerLabel("ACE");
        c.setMaxUses(1);
        c.setUsesCount(1);
        c.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        c.setActive(true);
        c.setCreatedAt(Instant.now());
        c.setCreatedByUserId(owner.getId());
        promoCodeRepository.save(c);

        mockMvc.perform(post("/api/v1/workspaces/" + workspace.getId() + "/billing/promo-codes/redeem")
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"FULL\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROMO_CODE_EXHAUSTED"));
    }

    // C10 — 2 redemptions du même workspace → 2e refusée
    @Test
    void redeem_sameWorkspaceTwice_secondAttemptReturns409AlreadyRedeemed() throws Exception {
        seedCode("ACE1", PromoCodeType.TRIAL_EXTENSION, 30, 100, true);
        seedCode("ACE2", PromoCodeType.TRIAL_EXTENSION, 30, 100, true);

        mockMvc.perform(post("/api/v1/workspaces/" + workspace.getId() + "/billing/promo-codes/redeem")
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ACE1\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/workspaces/" + workspace.getId() + "/billing/promo-codes/redeem")
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ACE2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKSPACE_ALREADY_REDEEMED_TRIAL_EXTENSION"));
    }

    // C11 — Workspace plan payant → 409
    @Test
    void redeem_paidWorkspace_returns409NotTrialing() throws Exception {
        Subscription sub = subscriptionRepository.findByWorkspaceId(workspace.getId()).orElseThrow();
        sub.setPlanCode("SOLO");
        subscriptionRepository.save(sub);

        seedCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100, true);

        mockMvc.perform(post("/api/v1/workspaces/" + workspace.getId() + "/billing/promo-codes/redeem")
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ACE2026\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKSPACE_NOT_TRIALING"));
    }

    // C12 — Workspace dont l'essai est expiré → 409
    @Test
    void redeem_workspaceTrialExpired_returns409NotTrialing() throws Exception {
        Subscription sub = subscriptionRepository.findByWorkspaceId(workspace.getId()).orElseThrow();
        sub.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        subscriptionRepository.save(sub);

        seedCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100, true);

        mockMvc.perform(post("/api/v1/workspaces/" + workspace.getId() + "/billing/promo-codes/redeem")
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ACE2026\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKSPACE_NOT_TRIALING"));
    }

    // C13 — Utilisateur pas membre du workspace cible → 403
    @Test
    void redeem_otherWorkspace_returns403ForbiddenWorkspace() throws Exception {
        // Crée un autre workspace dont l'owner n'est PAS membre.
        User otherOwner = newUser("other-" + UUID.randomUUID() + "@example.com", false);
        Workspace otherWorkspace = newWorkspace("Other", "FREE", otherOwner);
        newMember(otherWorkspace, otherOwner, "OWNER", true);
        newSubscription(otherWorkspace.getId(), "FREE", Instant.now().plus(5, ChronoUnit.DAYS));

        seedCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100, true);

        mockMvc.perform(post("/api/v1/workspaces/" + otherWorkspace.getId()
                        + "/billing/promo-codes/redeem")
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ACE2026\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_WORKSPACE"));

        // Vérifie qu'aucune redemption n'a touché l'autre workspace.
        assertThat(redemptionRepository.findAll()).isEmpty();
    }

    // C16 — type STRIPE_DISCOUNT → 409 PROMO_CODE_TYPE_NOT_SUPPORTED_YET
    @Test
    void redeem_stripeDiscountType_returns409TypeNotSupportedYet() throws Exception {
        seedCode("DISC2026", PromoCodeType.STRIPE_DISCOUNT, null, 100, true);

        mockMvc.perform(post("/api/v1/workspaces/" + workspace.getId() + "/billing/promo-codes/redeem")
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DISC2026\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROMO_CODE_TYPE_NOT_SUPPORTED_YET"));
    }

    @Test
    void redeem_unknownCode_returns404NotFound() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/" + workspace.getId() + "/billing/promo-codes/redeem")
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"UNKNOWN\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMO_CODE_NOT_FOUND"));
    }

    @Test
    void redeem_noAuth_returns401() throws Exception {
        seedCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100, true);

        mockMvc.perform(post("/api/v1/workspaces/" + workspace.getId() + "/billing/promo-codes/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ACE2026\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void redeem_blankCode_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/" + workspace.getId() + "/billing/promo-codes/redeem")
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // C14 — Concurrence : 2 threads, maxUses=1, exactement 1 succès
    @Test
    void redeem_concurrentRedemption_exactlyOneSucceedsOnMaxUsesOne() throws Exception {
        seedCode("RACE", PromoCodeType.TRIAL_EXTENSION, 30, 1, true);

        // 2 workspaces distincts pour éviter le blocage anti-abus (1 TRIAL_EXTENSION
        // par workspace à vie). On garde l'OWNER comme membre des deux pour simplifier.
        Workspace ws2 = newWorkspace("Race Workspace 2", "FREE", owner);
        newMember(ws2, owner, "OWNER", false);
        newSubscription(ws2.getId(), "FREE", Instant.now().plus(5, ChronoUnit.DAYS));

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exhaustedCount = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // On invoque directement le service plutôt que MockMvc pour des semantics
        // transactionnelles fiables sous load test (MockMvc gère mal les threads
        // parallèles partageant le même contexte de sécurité de test).
        Callable<Void> task1 = () -> {
            try {
                promoCodeService.redeemTrialExtension(workspace.getId(),
                        new RedeemRequest("RACE"),
                        (org.springframework.security.oauth2.core.oidc.user.OidcUser) ownerAuth.getPrincipal(),
                        "GOOGLE", ownerAuth);
                successCount.incrementAndGet();
            } catch (PromoCodeException e) {
                if (e.getCode() == PromoCodeErrorCode.PROMO_CODE_EXHAUSTED) {
                    exhaustedCount.incrementAndGet();
                }
            }
            return null;
        };
        Callable<Void> task2 = () -> {
            try {
                promoCodeService.redeemTrialExtension(ws2.getId(),
                        new RedeemRequest("RACE"),
                        (org.springframework.security.oauth2.core.oidc.user.OidcUser) ownerAuth.getPrincipal(),
                        "GOOGLE", ownerAuth);
                successCount.incrementAndGet();
            } catch (PromoCodeException e) {
                if (e.getCode() == PromoCodeErrorCode.PROMO_CODE_EXHAUSTED) {
                    exhaustedCount.incrementAndGet();
                }
            }
            return null;
        };

        Future<Void> f1 = pool.submit(task1);
        Future<Void> f2 = pool.submit(task2);
        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        // Garde-fou atomique UPDATE WHERE uses_count < max_uses → exactement 1 succès.
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(exhaustedCount.get()).isEqualTo(1);
        assertThat(redemptionRepository.findAll()).hasSize(1);
    }

    // ===== Helpers =====

    private User newUser(String email, boolean superAdmin) {
        User u = new User();
        u.setEmail(email);
        u.setStatus("ACTIVE");
        u.setSuperAdmin(superAdmin);
        return userRepository.save(u);
    }

    private void newAuthAccount(User user, String sub) {
        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId(sub);
        authAccountRepository.save(account);
    }

    private Workspace newWorkspace(String name, String planCode, User owner) {
        Workspace ws = new Workspace();
        ws.setName(name);
        ws.setSlug(name.toLowerCase().replace(" ", "-") + "-" + UUID.randomUUID());
        ws.setOwner(owner);
        ws.setPlanCode(planCode);
        ws.setStatus("ACTIVE");
        ws.setLegalDomain("DROIT_DU_TRAVAIL");
        return workspaceRepository.save(ws);
    }

    private void newMember(Workspace ws, User user, String role, boolean primary) {
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);
        member.setUser(user);
        member.setMemberRole(role);
        member.setPrimary(primary);
        workspaceMemberRepository.save(member);
    }

    private Subscription newSubscription(UUID workspaceId, String planCode, Instant expiresAt) {
        Subscription sub = new Subscription();
        sub.setWorkspaceId(workspaceId);
        sub.setPlanCode(planCode);
        sub.setStatus("ACTIVE");
        sub.setStartedAt(Instant.now().minus(7, ChronoUnit.DAYS));
        sub.setExpiresAt(expiresAt);
        return subscriptionRepository.save(sub);
    }

    private PromoCode seedCode(String code, PromoCodeType type, Integer valueDays,
                               int maxUses, boolean active) {
        PromoCode c = new PromoCode();
        c.setCode(code);
        c.setType(type);
        c.setValueDays(valueDays);
        c.setPartnerLabel("ACE");
        c.setMaxUses(maxUses);
        c.setUsesCount(0);
        c.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        c.setActive(active);
        c.setCreatedAt(Instant.now());
        c.setCreatedByUserId(owner.getId());
        return promoCodeRepository.save(c);
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
