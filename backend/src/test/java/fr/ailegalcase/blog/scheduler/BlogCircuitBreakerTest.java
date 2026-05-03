package fr.ailegalcase.blog.scheduler;

import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlogCircuitBreakerTest {

    private final BlogArticleRepository repository = mock(BlogArticleRepository.class);
    private final BlogCircuitBreaker breaker = new BlogCircuitBreaker(repository, 3, 15);

    @Test
    void zero_returnsOk() {
        when(repository.countByStatusAndPublishedAtAfter(eq(BlogArticleStatus.PUBLISHED), any(Instant.class)))
                .thenReturn(0L);
        assertThat(breaker.canGenerateNow()).isInstanceOf(CircuitBreakerVerdict.Ok.class);
    }

    @Test
    void atDailyCap_returnsDailyLimit() {
        when(repository.countByStatusAndPublishedAtAfter(eq(BlogArticleStatus.PUBLISHED), any(Instant.class)))
                .thenReturn(3L);
        CircuitBreakerVerdict v = breaker.canGenerateNow();
        assertThat(v).isInstanceOf(CircuitBreakerVerdict.DailyLimit.class);
        assertThat(((CircuitBreakerVerdict.DailyLimit) v).publishedToday()).isEqualTo(3);
    }

    @Test
    void belowDailyButAtWeeklyCap_returnsWeeklyLimit() {
        when(repository.countByStatusAndPublishedAtAfter(eq(BlogArticleStatus.PUBLISHED), any(Instant.class)))
                .thenReturn(2L) // daily check (1st call)
                .thenReturn(15L); // weekly check (2nd call)

        CircuitBreakerVerdict v = breaker.canGenerateNow();
        assertThat(v).isInstanceOf(CircuitBreakerVerdict.WeeklyLimit.class);
    }

    @Test
    void belowBothCaps_returnsOk() {
        when(repository.countByStatusAndPublishedAtAfter(eq(BlogArticleStatus.PUBLISHED), any(Instant.class)))
                .thenReturn(1L)
                .thenReturn(7L);

        assertThat(breaker.canGenerateNow()).isInstanceOf(CircuitBreakerVerdict.Ok.class);
    }
}
