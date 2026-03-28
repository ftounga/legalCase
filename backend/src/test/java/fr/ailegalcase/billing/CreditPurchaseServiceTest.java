package fr.ailegalcase.billing;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreditPurchaseServiceTest {

    private final CreditPurchaseRepository repo = mock(CreditPurchaseRepository.class);
    private final CreditPurchaseService service = new CreditPurchaseService(repo);

    // C-01 : aucun achat → 0 tokens
    @Test
    void getTotalTokensBought_noCredits_returnsZero() {
        UUID wsId = UUID.randomUUID();
        when(repo.sumTokensBoughtByWorkspaceId(wsId)).thenReturn(0L);
        assertThat(service.getTotalTokensBought(wsId)).isZero();
    }

    // C-02 : un achat → retourne le total
    @Test
    void getTotalTokensBought_oneCredit_returnsTotal() {
        UUID wsId = UUID.randomUUID();
        when(repo.sumTokensBoughtByWorkspaceId(wsId)).thenReturn(1_000_000L);
        assertThat(service.getTotalTokensBought(wsId)).isEqualTo(1_000_000L);
    }

    // C-03 : record nouveau → persiste
    @Test
    void record_newSession_persists() {
        when(repo.findByStripeSessionId("sess_1")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.record(UUID.randomUUID(), 1_000_000L, 990, "sess_1");
        verify(repo).save(any(CreditPurchase.class));
    }

    // C-04 : record doublon (même stripeSessionId) → idempotent
    @Test
    void record_duplicateSession_isIdempotent() {
        CreditPurchase existing = new CreditPurchase();
        when(repo.findByStripeSessionId("sess_dup")).thenReturn(Optional.of(existing));
        service.record(UUID.randomUUID(), 1_000_000L, 990, "sess_dup");
        verify(repo, never()).save(any());
    }
}
