package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SF-156-01 — Tests d'intégration HTTP de la création d'un workspace
 * supplémentaire.
 *
 * <p>Note : Stripe est désactivé dans la config test (pas de clé), donc le
 * workspace est créé en {@code ACTIVE} immédiatement par le service (mode
 * dev). Cela permet de tester le gate plan, la validation, l'isolation
 * cross-workspace et le guard ACTIVE sans dépendre de Stripe.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class WorkspaceControllerSf15601IT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;

    // I-156-01 (CA1) : OWNER TEAM, plan TEAM → 201, workspace créé
    @Test
    void POST_workspaces_ownerTeam_returns201() throws Exception {
        TestUser u = seedOwnerWithPlan("sf156-team@example.com", "g-sf156-team", "TEAM");

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Cabinet Bordeaux\", \"legalDomain\": \"DROIT_DU_TRAVAIL\", \"country\": \"FRANCE\", \"plan\": \"TEAM\"}")
                        .with(authentication(u.auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workspaceId").isNotEmpty())
                .andExpect(jsonPath("$.planCode").value("TEAM"))
                .andExpect(jsonPath("$.status").exists());

        // Vérifier que le 2ème workspace est bien persisté.
        long count = workspaceRepository.findAll().stream()
                .filter(w -> "CABINET BORDEAUX".equals(w.getName())).count();
        assertThat(count).isEqualTo(1);
    }

    // I-156-02 (CA2) : OWNER FREE → 403
    @Test
    void POST_workspaces_ownerFree_returns403() throws Exception {
        TestUser u = seedOwnerWithPlan("sf156-free@example.com", "g-sf156-free", "FREE");

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Cabinet X\", \"legalDomain\": \"DROIT_DU_TRAVAIL\", \"country\": \"FRANCE\", \"plan\": \"TEAM\"}")
                        .with(authentication(u.auth)))
                .andExpect(status().isForbidden());
    }

    // I-156-03 (CA3) : OWNER SOLO → 403
    @Test
    void POST_workspaces_ownerSolo_returns403() throws Exception {
        TestUser u = seedOwnerWithPlan("sf156-solo@example.com", "g-sf156-solo", "SOLO");

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Cabinet X\", \"legalDomain\": \"DROIT_DU_TRAVAIL\", \"country\": \"FRANCE\", \"plan\": \"TEAM\"}")
                        .with(authentication(u.auth)))
                .andExpect(status().isForbidden());
    }

    // I-156-04 (CA4) : plan FREE dans le body → 400 (validation @Pattern)
    @Test
    void POST_workspaces_planFree_returns400() throws Exception {
        TestUser u = seedOwnerWithPlan("sf156-planfree@example.com", "g-sf156-pf", "TEAM");

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Cabinet X\", \"legalDomain\": \"DROIT_DU_TRAVAIL\", \"country\": \"FRANCE\", \"plan\": \"FREE\"}")
                        .with(authentication(u.auth)))
                .andExpect(status().isBadRequest());
    }

    // I-156-04b (CA4) : plan SOLO dans le body → 400
    @Test
    void POST_workspaces_planSolo_returns400() throws Exception {
        TestUser u = seedOwnerWithPlan("sf156-plansolo@example.com", "g-sf156-ps", "TEAM");

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Cabinet X\", \"legalDomain\": \"DROIT_DU_TRAVAIL\", \"country\": \"FRANCE\", \"plan\": \"SOLO\"}")
                        .with(authentication(u.auth)))
                .andExpect(status().isBadRequest());
    }

    // I-156-04c (CA4) : plan INVALID → 400
    @Test
    void POST_workspaces_planInvalid_returns400() throws Exception {
        TestUser u = seedOwnerWithPlan("sf156-planinv@example.com", "g-sf156-pi", "TEAM");

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Cabinet X\", \"legalDomain\": \"DROIT_DU_TRAVAIL\", \"country\": \"FRANCE\", \"plan\": \"ENTERPRISE\"}")
                        .with(authentication(u.auth)))
                .andExpect(status().isBadRequest());
    }

    // I-156-05 (CA8) : un POST /api/v1/case-files sur un workspace
    // PENDING_PAYMENT (primary) → 409.
    @Test
    void POST_caseFiles_onPendingPaymentWorkspace_returns409() throws Exception {
        TestUser u = seedOwnerWithPlan("sf156-pp@example.com", "g-sf156-pp", "TEAM");

        // Bascule explicitement le workspace primary en PENDING_PAYMENT.
        Workspace ws = u.member.getWorkspace();
        ws.setStatus(WorkspaceStatus.PENDING_PAYMENT);
        workspaceRepository.save(ws);

        mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Dossier test\"}")
                        .with(authentication(u.auth)))
                .andExpect(status().isConflict());
    }

    // I-156-06 (CA9) : isolation cross-workspace — un autre OWNER ne voit pas
    // le workspace PENDING_PAYMENT créé par quelqu'un d'autre.
    @Test
    void POST_workspaces_isolation_otherOwnerSeesNothing() throws Exception {
        TestUser ownerA = seedOwnerWithPlan("sf156-owA@example.com", "g-sf156-owA", "TEAM");

        // OwnerA crée un workspace PENDING_PAYMENT (en mode test, Stripe
        // désactivé → workspace ACTIVE, on force après coup en PENDING_PAYMENT
        // pour valider l'isolation indépendamment de Stripe).
        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Cabinet B\", \"legalDomain\": \"DROIT_DU_TRAVAIL\", \"country\": \"FRANCE\", \"plan\": \"TEAM\"}")
                        .with(authentication(ownerA.auth)))
                .andExpect(status().isCreated());

        Workspace wsB = workspaceRepository.findAll().stream()
                .filter(w -> "CABINET B".equals(w.getName()))
                .findFirst().orElseThrow();
        wsB.setStatus(WorkspaceStatus.PENDING_PAYMENT);
        workspaceRepository.save(wsB);

        // Un autre OWNER (OwnerC) ne doit pas voir wsB.
        TestUser ownerC = seedOwnerWithPlan("sf156-owC@example.com", "g-sf156-owC", "TEAM");

        mockMvc.perform(get("/api/v1/workspaces")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(ownerC.auth)))
                .andExpect(status().isOk())
                // OwnerC voit uniquement son propre workspace (le seed).
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value(ownerC.member.getWorkspace().getName()));
    }

    // I-156-07 (CA9, vue depuis ownerA) : l'OWNER qui a créé wsB en
    // PENDING_PAYMENT le voit dans sa liste (banderole côté UI), mais il
    // reste sur son workspace primary — pas d'effet de bord sur ses dossiers.
    @Test
    void POST_workspaces_isolation_creatorSeesPendingPaymentInList() throws Exception {
        TestUser ownerA = seedOwnerWithPlan("sf156-creator@example.com", "g-sf156-creator", "TEAM");

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Cabinet Pending\", \"legalDomain\": \"DROIT_DU_TRAVAIL\", \"country\": \"FRANCE\", \"plan\": \"TEAM\"}")
                        .with(authentication(ownerA.auth)))
                .andExpect(status().isCreated());

        // Force le 2e workspace en PENDING_PAYMENT.
        Workspace wsB = workspaceRepository.findAll().stream()
                .filter(w -> "CABINET PENDING".equals(w.getName()))
                .findFirst().orElseThrow();
        wsB.setStatus(WorkspaceStatus.PENDING_PAYMENT);
        workspaceRepository.save(wsB);

        // OwnerA voit 2 workspaces dans sa liste (son original + le pending).
        mockMvc.perform(get("/api/v1/workspaces")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(ownerA.auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private record TestUser(User user, AuthAccount account, WorkspaceMember member,
                            OAuth2AuthenticationToken auth) {}

    private TestUser seedOwnerWithPlan(String email, String sub, String planCode) {
        User user = new User();
        user.setEmail(email);
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId(sub);
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("WS-" + email.toUpperCase());
        workspace.setSlug("slug-" + sub + "-" + System.nanoTime());
        workspace.setOwner(user);
        workspace.setPlanCode(planCode);
        workspace.setStatus(WorkspaceStatus.ACTIVE);
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setCountry("FRANCE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        // La subscription contrôle le plan effectif lu par PlanLimitService.
        Subscription sub2 = new Subscription();
        sub2.setWorkspaceId(workspace.getId());
        sub2.setPlanCode(planCode);
        sub2.setStatus("ACTIVE");
        sub2.setStartedAt(Instant.now());
        subscriptionRepository.save(sub2);

        return new TestUser(user, account, member, buildGoogleAuth(sub, email));
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
