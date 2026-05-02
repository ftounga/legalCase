package fr.ailegalcase.backlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BacklogSyncSchedulerTest {

    private BacklogSyncService syncService;
    private BacklogSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        syncService = mock(BacklogSyncService.class);
        scheduler = new BacklogSyncScheduler(syncService);
    }

    @Test
    void syncPeriodically_callsSyncWithScheduledTrigger() {
        scheduler.syncPeriodically();

        ArgumentCaptor<SyncTrigger> captor = ArgumentCaptor.forClass(SyncTrigger.class);
        verify(syncService, times(1)).sync(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue()).isEqualTo(SyncTrigger.SCHEDULED);
    }

    @Test
    void syncOnStartup_callsSyncWithStartupTrigger() {
        scheduler.syncOnStartup();

        ArgumentCaptor<SyncTrigger> captor = ArgumentCaptor.forClass(SyncTrigger.class);
        verify(syncService, times(1)).sync(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue()).isEqualTo(SyncTrigger.STARTUP);
    }

    @Test
    void exception_isLoggedAndDoesNotPropagate_whenScheduled() {
        when(syncService.sync(SyncTrigger.SCHEDULED))
                .thenThrow(new BacklogSyncService.BacklogSyncException("boom", new RuntimeException("inner")));

        assertThatCode(() -> scheduler.syncPeriodically()).doesNotThrowAnyException();
        verify(syncService, times(1)).sync(SyncTrigger.SCHEDULED);
    }

    @Test
    void exception_isLoggedAndDoesNotPropagate_whenStartup() {
        when(syncService.sync(SyncTrigger.STARTUP))
                .thenThrow(new BacklogSyncService.BacklogSyncException("boom", new RuntimeException("inner")));

        assertThatCode(() -> scheduler.syncOnStartup()).doesNotThrowAnyException();
        verify(syncService, times(1)).sync(SyncTrigger.STARTUP);
    }
}
