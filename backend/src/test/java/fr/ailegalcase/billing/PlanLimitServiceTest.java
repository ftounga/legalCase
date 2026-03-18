package fr.ailegalcase.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanLimitServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private PlanLimitService service;

    @BeforeEach
    void setUp() {
        service = new PlanLimitService(subscriptionRepository);
    }

    @Test
    void getMaxOpenCaseFiles_starter_returns3() {
        assertThat(service.getMaxOpenCaseFiles("STARTER")).isEqualTo(3);
    }

    @Test
    void getMaxOpenCaseFiles_pro_returns20() {
        assertThat(service.getMaxOpenCaseFiles("PRO")).isEqualTo(20);
    }

    @Test
    void getMaxOpenCaseFiles_unknown_returnsStarterDefault() {
        assertThat(service.getMaxOpenCaseFiles("UNKNOWN")).isEqualTo(3);
    }

    @Test
    void getMaxOpenCaseFilesForWorkspace_withStarterSubscription_returns3() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("STARTER");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));

        assertThat(service.getMaxOpenCaseFilesForWorkspace(workspaceId)).isEqualTo(3);
    }

    @Test
    void getMaxOpenCaseFilesForWorkspace_withProSubscription_returns20() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));

        assertThat(service.getMaxOpenCaseFilesForWorkspace(workspaceId)).isEqualTo(20);
    }

    @Test
    void getMaxOpenCaseFilesForWorkspace_noSubscription_returnsMaxValue() {
        UUID workspaceId = UUID.randomUUID();
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.empty());

        assertThat(service.getMaxOpenCaseFilesForWorkspace(workspaceId)).isEqualTo(Integer.MAX_VALUE);
    }
}
