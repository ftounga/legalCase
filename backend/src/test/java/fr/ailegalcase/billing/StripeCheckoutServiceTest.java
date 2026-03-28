package fr.ailegalcase.billing;

import com.stripe.exception.AuthenticationException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeCheckoutServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private StripeCustomerService stripeCustomerService;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private OidcUser oidcUser;

    private UUID workspaceId;
    private Subscription sub;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();

        User user = new User();
        user.setEmail("user@test.com");

        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);

        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);

        sub = new Subscription();
        sub.setWorkspaceId(workspaceId);
        sub.setStripeCustomerId("cus_existing");

        lenient().when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        lenient().when(workspaceMemberRepository.findByUserAndPrimaryTrue(any()))
                .thenReturn(Optional.of(member));
        lenient().when(subscriptionRepository.findByWorkspaceId(workspaceId))
                .thenReturn(Optional.of(sub));
    }

    private StripeCheckoutService buildService(boolean enabled) {
        return new StripeCheckoutService(
                enabled, "sk_test_fake",
                "price_solo", "price_team", "price_pro",
                "http://localhost:4200",
                subscriptionRepository, stripeCustomerService,
                currentUserResolver, workspaceMemberRepository);
    }

    // U-01 : Stripe désactivé → 503
    @Test
    void createCheckoutSession_stripeDisabled_throws503() {
        StripeCheckoutService service = buildService(false);

        assertThatThrownBy(() -> service.createCheckoutSession("SOLO", oidcUser, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // U-02 : planCode inconnu → 400
    @Test
    void createCheckoutSession_invalidPlanCode_throws400() {
        StripeCheckoutService service = buildService(true);

        assertThatThrownBy(() -> service.createCheckoutSession("UNKNOWN", oidcUser, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // U-03 : nominal SOLO → retourne checkoutUrl
    @Test
    void createCheckoutSession_solo_returnsCheckoutUrl() throws Exception {
        StripeCheckoutService service = buildService(true);

        Session mockSession = mock(Session.class);
        when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/solo");

        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            String url = service.createCheckoutSession("SOLO", oidcUser, "google", null);

            assertThat(url).isEqualTo("https://checkout.stripe.com/solo");
        }
    }

    // U-04 : nominal PRO → retourne checkoutUrl
    @Test
    void createCheckoutSession_pro_returnsCheckoutUrl() throws Exception {
        StripeCheckoutService service = buildService(true);

        Session mockSession = mock(Session.class);
        when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pro");

        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            String url = service.createCheckoutSession("PRO", oidcUser, "google", null);

            assertThat(url).isEqualTo("https://checkout.stripe.com/pro");
        }
    }

    // U-05 : stripe_customer_id absent → customer créé avant session
    @Test
    void createCheckoutSession_noCustomerId_createsCustomerFirst() throws Exception {
        sub.setStripeCustomerId(null);
        when(stripeCustomerService.createCustomer(any(), any()))
                .thenReturn(Optional.of("cus_new"));

        StripeCheckoutService service = buildService(true);

        Session mockSession = mock(Session.class);
        when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay");

        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            service.createCheckoutSession("SOLO", oidcUser, "google", null);

            verify(stripeCustomerService).createCustomer(any(), eq(workspaceId));
            verify(subscriptionRepository, atLeastOnce()).save(sub);
        }
    }

    // U-06 : erreur Stripe → 502
    @Test
    void createCheckoutSession_stripeException_throws502() throws Exception {
        StripeCheckoutService service = buildService(true);

        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenThrow(new AuthenticationException("Invalid key", "req_x", null, 401));

            assertThatThrownBy(() -> service.createCheckoutSession("SOLO", oidcUser, "google", null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_GATEWAY);
        }
    }
}
