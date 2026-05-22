package fr.ailegalcase.jurisprudencemapping;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JurisprudenceWatchSchedulerTest {

    @Test
    void runMonthly_skipsWhenDisabled() {
        JurisprudenceWatchService service = mock(JurisprudenceWatchService.class);
        JurisprudenceWatchScheduler scheduler = new JurisprudenceWatchScheduler(service, false);

        scheduler.runMonthly();

        verify(service, never()).runMonthlyWatch();
    }

    @Test
    void runMonthly_invokesServiceWhenEnabled() {
        JurisprudenceWatchService service = mock(JurisprudenceWatchService.class);
        JurisprudenceWatchScheduler scheduler = new JurisprudenceWatchScheduler(service, true);

        scheduler.runMonthly();

        verify(service, times(1)).runMonthlyWatch();
    }

    @Test
    void runMonthly_doesNotThrowOnServiceFailure() {
        JurisprudenceWatchService service = mock(JurisprudenceWatchService.class);
        when(service.runMonthlyWatch()).thenThrow(new RuntimeException("boom"));
        JurisprudenceWatchScheduler scheduler = new JurisprudenceWatchScheduler(service, true);

        scheduler.runMonthly(); // must not throw

        verify(service, times(1)).runMonthlyWatch();
    }
}
