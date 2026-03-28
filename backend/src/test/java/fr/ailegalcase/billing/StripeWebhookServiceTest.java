package fr.ailegalcase.billing;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private StripeWebhookService service;

    @BeforeEach
    void setUp() {
        service = new StripeWebhookService(subscriptionRepository, "price_solo_test", "price_team_test", "price_pro_test");
    }

    // U-01 : checkout.session.completed → plan mis à jour
    @Test
    void handleEvent_checkoutCompleted_updatesPlan() {
        fr.ailegalcase.billing.Subscription sub = new fr.ailegalcase.billing.Subscription();
        sub.setPlanCode("FREE");
        when(subscriptionRepository.findByStripeCustomerId("cus_abc")).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Session session = mock(Session.class);
        when(session.getCustomer()).thenReturn("cus_abc");
        when(session.getSubscription()).thenReturn("sub_123");
        when(session.getMetadata()).thenReturn(java.util.Map.of("plan_code", "SOLO"));

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        try { when(deserializer.deserializeUnsafe()).thenReturn((StripeObject) session); }
        catch (com.stripe.exception.StripeException ignored) {}

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        service.handleEvent(event);

        ArgumentCaptor<fr.ailegalcase.billing.Subscription> captor =
                ArgumentCaptor.forClass(fr.ailegalcase.billing.Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getPlanCode()).isEqualTo("SOLO");
        assertThat(captor.getValue().getStripeSubscriptionId()).isEqualTo("sub_123");
        assertThat(captor.getValue().getExpiresAt()).isNull();
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    // U-02 : customer.subscription.deleted → FREE + expires_at = now()
    @Test
    void handleEvent_subscriptionDeleted_downgradesToFree() {
        fr.ailegalcase.billing.Subscription sub = new fr.ailegalcase.billing.Subscription();
        sub.setPlanCode("SOLO");
        sub.setStripeSubscriptionId("sub_123");
        when(subscriptionRepository.findByStripeCustomerId("cus_abc")).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        when(stripeSub.getCustomer()).thenReturn("cus_abc");

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        try { when(deserializer.deserializeUnsafe()).thenReturn((StripeObject) stripeSub); }
        catch (com.stripe.exception.StripeException ignored) {}

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("customer.subscription.deleted");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        service.handleEvent(event);

        ArgumentCaptor<fr.ailegalcase.billing.Subscription> captor =
                ArgumentCaptor.forClass(fr.ailegalcase.billing.Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getPlanCode()).isEqualTo("FREE");
        assertThat(captor.getValue().getStripeSubscriptionId()).isNull();
        assertThat(captor.getValue().getExpiresAt()).isNotNull();
        assertThat(captor.getValue().getExpiresAt()).isBefore(Instant.now().plusSeconds(1));
    }

    // U-03 : customer inconnu → log WARN, pas d'exception, pas de save
    @Test
    void handleEvent_unknownCustomer_noException() {
        when(subscriptionRepository.findByStripeCustomerId("cus_unknown")).thenReturn(java.util.Optional.empty());

        Session session = mock(Session.class, withSettings().lenient());
        when(session.getCustomer()).thenReturn("cus_unknown");
        when(session.getSubscription()).thenReturn("sub_x");
        when(session.getMetadata()).thenReturn(java.util.Map.of());

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        try { when(deserializer.deserializeUnsafe()).thenReturn((StripeObject) session); }
        catch (com.stripe.exception.StripeException ignored) {}

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        service.handleEvent(event);

        verify(subscriptionRepository, never()).save(any());
    }

    // U-04 : événement non géré → ignoré sans erreur
    @Test
    void handleEvent_unknownEventType_ignored() {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn("invoice.payment_failed");

        service.handleEvent(event);

        verify(subscriptionRepository, never()).findByStripeCustomerId(any());
        verify(subscriptionRepository, never()).save(any());
    }

    // U-05 : resolvePlanCodeFromPriceId
    @Test
    void resolvePlanCodeFromPriceId_soloPriceId_returnsSolo() {
        assertThat(service.resolvePlanCodeFromPriceId("price_solo_test")).isEqualTo("SOLO");
    }

    @Test
    void resolvePlanCodeFromPriceId_teamPriceId_returnsTeam() {
        assertThat(service.resolvePlanCodeFromPriceId("price_team_test")).isEqualTo("TEAM");
    }

    @Test
    void resolvePlanCodeFromPriceId_proPriceId_returnsPro() {
        assertThat(service.resolvePlanCodeFromPriceId("price_pro_test")).isEqualTo("PRO");
    }

    @Test
    void resolvePlanCodeFromPriceId_unknownPriceId_returnsSoloDefault() {
        assertThat(service.resolvePlanCodeFromPriceId("price_unknown")).isEqualTo("SOLO");
    }
}
