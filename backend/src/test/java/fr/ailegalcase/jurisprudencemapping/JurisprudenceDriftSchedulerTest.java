package fr.ailegalcase.jurisprudencemapping;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JurisprudenceDriftSchedulerTest {

    @Test
    void runDaily_skipsWhenDisabled() {
        JurisprudenceDriftService service = mock(JurisprudenceDriftService.class);
        JurisprudenceDriftScheduler scheduler = new JurisprudenceDriftScheduler(service, false);

        scheduler.runDaily();

        verify(service, never()).runDriftScan();
    }

    @Test
    void runDaily_invokesServiceWhenEnabled() {
        JurisprudenceDriftService service = mock(JurisprudenceDriftService.class);
        JurisprudenceDriftScheduler scheduler = new JurisprudenceDriftScheduler(service, true);

        scheduler.runDaily();

        verify(service, times(1)).runDriftScan();
    }

    @Test
    void runDaily_swallowsServiceException() {
        JurisprudenceDriftService service = mock(JurisprudenceDriftService.class);
        when(service.runDriftScan()).thenThrow(new RuntimeException("boom"));
        JurisprudenceDriftScheduler scheduler = new JurisprudenceDriftScheduler(service, true);

        scheduler.runDaily(); // must not throw

        verify(service, times(1)).runDriftScan();
    }
}
