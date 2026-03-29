package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingEmailSchedulerTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @InjectMocks private OnboardingEmailScheduler scheduler;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@example.com");
        user.setStatus("ACTIVE");
    }

    @Test
    void T1_j2_sendsOnboardingTipAnalysis() {
        when(userRepository.findByCreatedAtBetween(any(), any()))
                .thenAnswer(inv -> {
                    Instant from = inv.getArgument(0);
                    // Only respond for the 2-day slot
                    Instant now = Instant.now();
                    Instant expected = now.truncatedTo(ChronoUnit.DAYS).minus(2, ChronoUnit.DAYS);
                    return from.truncatedTo(ChronoUnit.DAYS).equals(expected) ? List.of(user) : List.of();
                });

        scheduler.sendDailyOnboardingEmails();

        verify(emailService).sendOnboardingTipAnalysis(user);
        verify(emailService, never()).sendOnboardingTipShare(any());
        verify(emailService, never()).sendOnboardingBeforeExpiry(any());
        verify(emailService, never()).sendOnboardingExpired(any());
    }

    @Test
    void T2_j2_skipIfAlreadySent() {
        when(userRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of());

        scheduler.sendDailyOnboardingEmails();

        verify(emailService, never()).sendOnboardingTipAnalysis(any());
    }

    @Test
    void T3_j5_sendsOnboardingTipShare() {
        when(userRepository.findByCreatedAtBetween(any(), any()))
                .thenAnswer(inv -> {
                    Instant from = inv.getArgument(0);
                    Instant now = Instant.now();
                    Instant expected = now.truncatedTo(ChronoUnit.DAYS).minus(5, ChronoUnit.DAYS);
                    return from.truncatedTo(ChronoUnit.DAYS).equals(expected) ? List.of(user) : List.of();
                });

        scheduler.sendDailyOnboardingEmails();

        verify(emailService).sendOnboardingTipShare(user);
        verify(emailService, never()).sendOnboardingTipAnalysis(any());
    }

    @Test
    void T4_j12_sendsOnboardingBeforeExpiry() {
        when(userRepository.findByCreatedAtBetween(any(), any()))
                .thenAnswer(inv -> {
                    Instant from = inv.getArgument(0);
                    Instant now = Instant.now();
                    Instant expected = now.truncatedTo(ChronoUnit.DAYS).minus(12, ChronoUnit.DAYS);
                    return from.truncatedTo(ChronoUnit.DAYS).equals(expected) ? List.of(user) : List.of();
                });

        scheduler.sendDailyOnboardingEmails();

        verify(emailService).sendOnboardingBeforeExpiry(user);
        verify(emailService, never()).sendOnboardingExpired(any());
    }

    @Test
    void T5_j15_sendsOnboardingExpired() {
        when(userRepository.findByCreatedAtBetween(any(), any()))
                .thenAnswer(inv -> {
                    Instant from = inv.getArgument(0);
                    Instant now = Instant.now();
                    Instant expected = now.truncatedTo(ChronoUnit.DAYS).minus(15, ChronoUnit.DAYS);
                    return from.truncatedTo(ChronoUnit.DAYS).equals(expected) ? List.of(user) : List.of();
                });

        scheduler.sendDailyOnboardingEmails();

        verify(emailService).sendOnboardingExpired(user);
        verify(emailService, never()).sendOnboardingBeforeExpiry(any());
    }
}
