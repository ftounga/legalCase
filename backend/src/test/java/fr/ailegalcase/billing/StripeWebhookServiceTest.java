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
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private CreditPurchaseService creditPurchaseService;

    private StripeWebhookService service;

    @BeforeEach
    void setUp() {
        service = new StripeWebhookService(subscriptionRepository, creditPurchaseService,
                "price_solo_test", "price_team_test", "price_pro_test",
                "price_tokens_1m_test", "price_tokens_5m_test", "price_tokens_20m_test");
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

    // U-05 : resolvePlanCodeFromPriceId — 1 price ID par plan
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

    // ── Topup payment ─────────────────────────────────────────────────────

    // W-08 : checkout.session.completed mode=payment → CreditPurchaseService.record() appelé
    @Test
    void handleEvent_topupPayment_recordsCredit() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        Session session = mock(Session.class);
        when(session.getMode()).thenReturn("payment");
        when(session.getId()).thenReturn("sess_topup_1");
        when(session.getMetadata()).thenReturn(java.util.Map.of(
                "workspace_id", workspaceId.toString(),
                "pack_code", "TOKENS_1M"));

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.deserializeUnsafe()).thenReturn((StripeObject) session);

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        service.handleEvent(event);

        verify(creditPurchaseService).record(
                eq(workspaceId),
                eq(TokenPack.TOKENS_1M.getTokens()),
                eq(TokenPack.TOKENS_1M.getAmountCents()),
                eq("sess_topup_1"));
        verify(subscriptionRepository, never()).save(any());
    }

    // W-09 : checkout.session.completed mode=payment, pack_code inconnu → ignoré silencieusement
    @Test
    void handleEvent_topupPayment_unknownPackCode_ignored() throws Exception {
        Session session = mock(Session.class);
        when(session.getMode()).thenReturn("payment");
        when(session.getId()).thenReturn("sess_bad");
        when(session.getMetadata()).thenReturn(java.util.Map.of(
                "workspace_id", UUID.randomUUID().toString(),
                "pack_code", "UNKNOWN_PACK"));

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.deserializeUnsafe()).thenReturn((StripeObject) session);

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        service.handleEvent(event);

        verify(creditPurchaseService, never()).record(any(), anyLong(), anyInt(), any());
    }

    // W-10 : checkout.session.completed mode=subscription → PAS de credit créé
    @Test
    void handleEvent_subscriptionMode_doesNotRecordCredit() throws Exception {
        fr.ailegalcase.billing.Subscription sub = new fr.ailegalcase.billing.Subscription();
        sub.setPlanCode("FREE");
        when(subscriptionRepository.findByStripeCustomerId("cus_sub")).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Session session = mock(Session.class);
        when(session.getMode()).thenReturn("subscription");
        when(session.getCustomer()).thenReturn("cus_sub");
        when(session.getSubscription()).thenReturn("sub_xyz");
        when(session.getMetadata()).thenReturn(java.util.Map.of("plan_code", "SOLO"));

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.deserializeUnsafe()).thenReturn((StripeObject) session);

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        service.handleEvent(event);

        verify(creditPurchaseService, never()).record(any(), anyLong(), anyInt(), any());
        verify(subscriptionRepository).save(any());
    }

    // SF-123-02 — customer.subscription.updated avec quantity différent → seatCount sync
    @Test
    void handleEvent_subscriptionUpdated_syncsSeatCountFromStripe() throws Exception {
        fr.ailegalcase.billing.Subscription sub = new fr.ailegalcase.billing.Subscription();
        sub.setPlanCode("TEAM");
        sub.setSeatCount(3);
        when(subscriptionRepository.findByStripeCustomerId("cus_team")).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        com.stripe.model.SubscriptionItem item = mock(com.stripe.model.SubscriptionItem.class);
        com.stripe.model.Price price = mock(com.stripe.model.Price.class);
        com.stripe.model.SubscriptionItemCollection items = mock(com.stripe.model.SubscriptionItemCollection.class);
        when(stripeSub.getCustomer()).thenReturn("cus_team");
        when(stripeSub.getId()).thenReturn("sub_t_123");
        when(stripeSub.getItems()).thenReturn(items);
        when(items.getData()).thenReturn(java.util.List.of(item));
        when(item.getPrice()).thenReturn(price);
        when(price.getId()).thenReturn("price_team_test");
        when(item.getQuantity()).thenReturn(5L);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.deserializeUnsafe()).thenReturn((StripeObject) stripeSub);

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("customer.subscription.updated");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        service.handleEvent(event);

        ArgumentCaptor<fr.ailegalcase.billing.Subscription> captor =
                ArgumentCaptor.forClass(fr.ailegalcase.billing.Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getSeatCount()).isEqualTo(5);
        assertThat(captor.getValue().getPlanCode()).isEqualTo("TEAM");
    }

    // SF-247-01 — customer.subscription.deleted → FREE + cancelAtPeriodEnd reset à false
    @Test
    void handleEvent_subscriptionDeleted_resetsCancelAtPeriodEnd() {
        fr.ailegalcase.billing.Subscription sub = new fr.ailegalcase.billing.Subscription();
        sub.setPlanCode("SOLO");
        sub.setStripeSubscriptionId("sub_123");
        sub.setCancelAtPeriodEnd(true);
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
        assertThat(captor.getValue().isCancelAtPeriodEnd()).isFalse();
    }

    // SF-247-01 — customer.subscription.updated avec cancel_at_period_end=true → synchro locale
    @Test
    void handleEvent_subscriptionUpdated_syncsCancelAtPeriodEnd() throws Exception {
        fr.ailegalcase.billing.Subscription sub = new fr.ailegalcase.billing.Subscription();
        sub.setPlanCode("SOLO");
        sub.setCancelAtPeriodEnd(false);
        when(subscriptionRepository.findByStripeCustomerId("cus_cancel")).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        com.stripe.model.SubscriptionItem item = mock(com.stripe.model.SubscriptionItem.class);
        com.stripe.model.Price price = mock(com.stripe.model.Price.class);
        com.stripe.model.SubscriptionItemCollection items = mock(com.stripe.model.SubscriptionItemCollection.class);
        when(stripeSub.getCustomer()).thenReturn("cus_cancel");
        when(stripeSub.getId()).thenReturn("sub_c_1");
        when(stripeSub.getItems()).thenReturn(items);
        when(items.getData()).thenReturn(java.util.List.of(item));
        when(item.getPrice()).thenReturn(price);
        when(price.getId()).thenReturn("price_solo_test");
        when(item.getQuantity()).thenReturn(1L);
        when(stripeSub.getCancelAtPeriodEnd()).thenReturn(Boolean.TRUE);
        when(stripeSub.getCurrentPeriodEnd()).thenReturn(1_900_000_000L);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.deserializeUnsafe()).thenReturn((StripeObject) stripeSub);

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("customer.subscription.updated");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        service.handleEvent(event);

        ArgumentCaptor<fr.ailegalcase.billing.Subscription> captor =
                ArgumentCaptor.forClass(fr.ailegalcase.billing.Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().isCancelAtPeriodEnd()).isTrue();
        assertThat(captor.getValue().getCurrentPeriodEnd())
                .isEqualTo(Instant.ofEpochSecond(1_900_000_000L));
    }
}
