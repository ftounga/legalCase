package fr.ailegalcase.billing;

import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.Price;
import com.stripe.model.StripeObject;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceRepository;
import fr.ailegalcase.workspace.WorkspaceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SF-156-01 — Tests des extensions webhook Stripe :
 * <ul>
 *   <li>{@code customer.subscription.created} → activation PENDING_PAYMENT → ACTIVE</li>
 *   <li>Idempotence sur double réception du même {@code subscription.id}</li>
 *   <li>{@code customer.subscription.deleted} sur workspace PENDING_PAYMENT → CANCELLED</li>
 *   <li>{@code invoice.payment_failed} sur workspace PENDING_PAYMENT → CANCELLED</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceSf15601Test {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private CreditPurchaseService creditPurchaseService;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private PromoCodeRepository promoCodeRepository;
    @Mock private PromoCodeRedemptionRepository promoCodeRedemptionRepository;

    private StripeWebhookService service;

    @BeforeEach
    void setUp() {
        service = new StripeWebhookService(subscriptionRepository, creditPurchaseService,
                workspaceRepository, promoCodeRepository, promoCodeRedemptionRepository,
                "price_solo_test", "price_team_test", "price_pro_test",
                "price_tokens_1m_test", "price_tokens_5m_test", "price_tokens_20m_test");
    }

    // W-156-01 : customer.subscription.created sur workspace PENDING_PAYMENT
    //            → status passe à ACTIVE, plan figé selon priceId.
    @Test
    void subscriptionCreated_pendingPayment_activates() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = newWorkspace(workspaceId, WorkspaceStatus.PENDING_PAYMENT, "TEAM");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.empty());

        Event event = buildSubscriptionCreatedEvent(workspaceId, "cus_abc", "sub_xyz", "price_team_test");

        try (MockedStatic<Customer> customerStatic = mockCustomerWithMetadata("cus_abc", workspaceId)) {
            service.handleEvent(event);
        }

        ArgumentCaptor<Workspace> wsCaptor = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(wsCaptor.capture());
        assertThat(wsCaptor.getValue().getStatus()).isEqualTo(WorkspaceStatus.ACTIVE);
        assertThat(wsCaptor.getValue().getPlanCode()).isEqualTo("TEAM");

        ArgumentCaptor<fr.ailegalcase.billing.Subscription> subCaptor =
                ArgumentCaptor.forClass(fr.ailegalcase.billing.Subscription.class);
        verify(subscriptionRepository).save(subCaptor.capture());
        assertThat(subCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(subCaptor.getValue().getStripeSubscriptionId()).isEqualTo("sub_xyz");
        assertThat(subCaptor.getValue().getStripeCustomerId()).isEqualTo("cus_abc");
    }

    // W-156-02 (CA5) : idempotence — double réception du même sub.id → no-op
    @Test
    void subscriptionCreated_alreadyActiveSameSubId_isIdempotent() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = newWorkspace(workspaceId, WorkspaceStatus.ACTIVE, "TEAM");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        fr.ailegalcase.billing.Subscription existing = new fr.ailegalcase.billing.Subscription();
        existing.setWorkspaceId(workspaceId);
        existing.setStripeSubscriptionId("sub_xyz");
        existing.setStatus("ACTIVE");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(existing));

        Event event = buildSubscriptionCreatedEvent(workspaceId, "cus_abc", "sub_xyz", "price_team_test");

        try (MockedStatic<Customer> customerStatic = mockCustomerWithMetadata("cus_abc", workspaceId)) {
            service.handleEvent(event);
        }

        verify(workspaceRepository, never()).save(any());
        verify(subscriptionRepository, never()).save(any());
    }

    // W-156-03 (CA6) : customer.subscription.deleted sur workspace PENDING_PAYMENT
    //                  → workspace bascule en CANCELLED (et pas downgrade FREE)
    @Test
    void subscriptionDeleted_pendingPayment_cancels() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = newWorkspace(workspaceId, WorkspaceStatus.PENDING_PAYMENT, "TEAM");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        fr.ailegalcase.billing.Subscription localSub = new fr.ailegalcase.billing.Subscription();
        localSub.setWorkspaceId(workspaceId);
        localSub.setPlanCode("TEAM");
        localSub.setStatus("PENDING_PAYMENT");
        localSub.setStripeCustomerId("cus_abc");
        when(subscriptionRepository.findByStripeCustomerId("cus_abc")).thenReturn(Optional.of(localSub));

        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        when(stripeSub.getCustomer()).thenReturn("cus_abc");

        Event event = wrap("customer.subscription.deleted", stripeSub);
        service.handleEvent(event);

        ArgumentCaptor<Workspace> wsCaptor = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(wsCaptor.capture());
        assertThat(wsCaptor.getValue().getStatus()).isEqualTo(WorkspaceStatus.CANCELLED);

        ArgumentCaptor<fr.ailegalcase.billing.Subscription> subCaptor =
                ArgumentCaptor.forClass(fr.ailegalcase.billing.Subscription.class);
        verify(subscriptionRepository).save(subCaptor.capture());
        assertThat(subCaptor.getValue().getStatus()).isEqualTo("CANCELLED");
    }

    // W-156-04 (CA6) : invoice.payment_failed sur workspace PENDING_PAYMENT
    //                  → CANCELLED
    @Test
    void invoicePaymentFailed_pendingPayment_cancels() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = newWorkspace(workspaceId, WorkspaceStatus.PENDING_PAYMENT, "TEAM");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        fr.ailegalcase.billing.Subscription localSub = new fr.ailegalcase.billing.Subscription();
        localSub.setWorkspaceId(workspaceId);
        localSub.setStripeCustomerId("cus_abc");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(localSub));

        Invoice invoice = mock(Invoice.class);
        when(invoice.getCustomer()).thenReturn("cus_abc");

        Event event = wrap("invoice.payment_failed", invoice);

        try (MockedStatic<Customer> customerStatic = mockCustomerWithMetadata("cus_abc", workspaceId)) {
            service.handleEvent(event);
        }

        ArgumentCaptor<Workspace> wsCaptor = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(wsCaptor.capture());
        assertThat(wsCaptor.getValue().getStatus()).isEqualTo(WorkspaceStatus.CANCELLED);
    }

    // W-156-05 : invoice.payment_failed sur workspace ACTIVE → ignoré (Stripe
    //            gère le dunning, on attend customer.subscription.deleted).
    @Test
    void invoicePaymentFailed_active_isIgnored() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = newWorkspace(workspaceId, WorkspaceStatus.ACTIVE, "TEAM");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        Invoice invoice = mock(Invoice.class);
        when(invoice.getCustomer()).thenReturn("cus_abc");

        Event event = wrap("invoice.payment_failed", invoice);

        try (MockedStatic<Customer> customerStatic = mockCustomerWithMetadata("cus_abc", workspaceId)) {
            service.handleEvent(event);
        }

        verify(workspaceRepository, never()).save(any());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Workspace newWorkspace(UUID id, String status, String plan) {
        Workspace w = new Workspace();
        try {
            var idField = Workspace.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(w, id);
        } catch (Exception e) { throw new RuntimeException(e); }
        w.setName("Test WS");
        w.setSlug("slug-" + id);
        w.setPlanCode(plan);
        w.setStatus(status);
        w.setLegalDomain("DROIT_DU_TRAVAIL");
        w.setCountry("FRANCE");
        return w;
    }

    private Event buildSubscriptionCreatedEvent(UUID workspaceId, String customerId,
                                                String subscriptionId, String priceId) throws Exception {
        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        SubscriptionItem item = mock(SubscriptionItem.class);
        Price price = mock(Price.class);
        SubscriptionItemCollection items = mock(SubscriptionItemCollection.class);
        when(stripeSub.getCustomer()).thenReturn(customerId);
        when(stripeSub.getId()).thenReturn(subscriptionId);
        when(stripeSub.getItems()).thenReturn(items);
        when(items.getData()).thenReturn(List.of(item));
        when(item.getPrice()).thenReturn(price);
        when(price.getId()).thenReturn(priceId);

        return wrap("customer.subscription.created", stripeSub);
    }

    private Event wrap(String type, StripeObject payload) throws Exception {
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.deserializeUnsafe()).thenReturn(payload);
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(type);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        return event;
    }

    private MockedStatic<Customer> mockCustomerWithMetadata(String customerId, UUID workspaceId) {
        MockedStatic<Customer> customerStatic = mockStatic(Customer.class);
        Customer customer = mock(Customer.class);
        when(customer.getMetadata()).thenReturn(Map.of("workspace_id", workspaceId.toString()));
        customerStatic.when(() -> Customer.retrieve(customerId)).thenReturn(customer);
        return customerStatic;
    }
}
