package fr.ailegalcase.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZombieJobResetSchedulerTest {

    @Mock private AnalysisJobRepository analysisJobRepository;

    private ZombieJobResetScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ZombieJobResetScheduler(analysisJobRepository);
    }

    // U-01 : resetZombies appelle le repo avec un staleBefore = now - 30min
    @Test
    void resetZombies_callsRepoWithCorrectStaleThreshold() {
        when(analysisJobRepository.forceFailZombieJobs(
                org.mockito.ArgumentMatchers.any(), anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(0);

        Instant before = Instant.now().minusSeconds(1);
        scheduler.resetZombies();
        Instant after = Instant.now().plusSeconds(1);

        ArgumentCaptor<Instant> staleBeforeCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(analysisJobRepository).forceFailZombieJobs(
                staleBeforeCaptor.capture(), anyString(), org.mockito.ArgumentMatchers.any());

        Instant captured = staleBeforeCaptor.getValue();
        // staleBefore doit être approximativement now - 30 min
        long minutesAgo = java.time.Duration.between(captured, Instant.now()).toMinutes();
        assertThat(minutesAgo).isBetween(29L, 31L);
    }

    // U-02 : le message d'erreur persiste "Zombie reset"
    @Test
    void resetZombies_errorMessageIsIdentifiable() {
        when(analysisJobRepository.forceFailZombieJobs(
                org.mockito.ArgumentMatchers.any(), anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(3);

        scheduler.resetZombies();

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(analysisJobRepository).forceFailZombieJobs(
                org.mockito.ArgumentMatchers.any(), messageCaptor.capture(), org.mockito.ArgumentMatchers.any());

        assertThat(messageCaptor.getValue()).contains("Zombie reset");
    }
}
