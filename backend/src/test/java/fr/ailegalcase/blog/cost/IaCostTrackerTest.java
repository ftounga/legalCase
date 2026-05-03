package fr.ailegalcase.blog.cost;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IaCostTrackerTest {

    private final BlogUsageEventRepository repository = mock(BlogUsageEventRepository.class);
    private final IaCostTracker tracker = new IaCostTracker(
            repository, new BigDecimal("30"), new BigDecimal("20"));

    @Test
    void canSpend_returnsTrue_whenSpentPlusEstimatedBelowCap() {
        when(repository.sumCostByProviderSince(eq(IaProvider.ANTHROPIC), any(Instant.class)))
                .thenReturn(new BigDecimal("25"));

        boolean result = tracker.canSpend(IaProvider.ANTHROPIC, new BigDecimal("3"));

        assertThat(result).isTrue();
    }

    @Test
    void canSpend_returnsFalse_whenSpentPlusEstimatedExceedsCap() {
        when(repository.sumCostByProviderSince(eq(IaProvider.ANTHROPIC), any(Instant.class)))
                .thenReturn(new BigDecimal("29.95"));

        boolean result = tracker.canSpend(IaProvider.ANTHROPIC, new BigDecimal("0.10"));

        assertThat(result).isFalse();
    }

    @Test
    void canSpend_returnsTrue_whenExactlyAtCap() {
        when(repository.sumCostByProviderSince(eq(IaProvider.ANTHROPIC), any(Instant.class)))
                .thenReturn(new BigDecimal("29.90"));

        boolean result = tracker.canSpend(IaProvider.ANTHROPIC, new BigDecimal("0.10"));

        assertThat(result).isTrue();
    }

    @Test
    void canSpend_isolatesProviders() {
        when(repository.sumCostByProviderSince(eq(IaProvider.OPENAI), any(Instant.class)))
                .thenReturn(BigDecimal.ZERO);

        boolean result = tracker.canSpend(IaProvider.OPENAI, new BigDecimal("0.05"));

        assertThat(result).isTrue();
    }

    @Test
    void record_persistsEventWithAllFields() {
        UUID articleId = UUID.randomUUID();
        tracker.record(IaProvider.ANTHROPIC, "claude-sonnet-4-6",
                BlogUsageEventType.ARTICLE_GENERATION, articleId,
                500, 4000, new BigDecimal("0.085000"));

        ArgumentCaptor<BlogUsageEvent> captor = ArgumentCaptor.forClass(BlogUsageEvent.class);
        verify(repository).save(captor.capture());
        BlogUsageEvent saved = captor.getValue();
        assertThat(saved.getProvider()).isEqualTo(IaProvider.ANTHROPIC);
        assertThat(saved.getModel()).isEqualTo("claude-sonnet-4-6");
        assertThat(saved.getEventType()).isEqualTo(BlogUsageEventType.ARTICLE_GENERATION);
        assertThat(saved.getBlogArticleId()).isEqualTo(articleId);
        assertThat(saved.getTokensInput()).isEqualTo(500);
        assertThat(saved.getTokensOutput()).isEqualTo(4000);
        assertThat(saved.getCostEur()).isEqualByComparingTo(new BigDecimal("0.085"));
    }

    @Test
    void monthlySpent_callsRepositoryWithStartOfMonth() {
        when(repository.sumCostByProviderSince(eq(IaProvider.ANTHROPIC), any(Instant.class)))
                .thenReturn(new BigDecimal("12.50"));

        BigDecimal spent = tracker.monthlySpent(IaProvider.ANTHROPIC);

        assertThat(spent).isEqualByComparingTo(new BigDecimal("12.50"));
    }

    @Test
    void canSpend_throwsWhenProviderNotConfigured() {
        IaCostTracker partial = new IaCostTracker(repository, new BigDecimal("30"), null);

        assertThatThrownBy(() -> partial.canSpend(IaProvider.OPENAI, BigDecimal.ONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPENAI");
    }
}
