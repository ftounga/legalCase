package fr.ailegalcase.timetracking;

import fr.ailegalcase.audit.AuditLog;
import fr.ailegalcase.audit.AuditLogRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.timetracking.dto.BillingRateRequest;
import fr.ailegalcase.timetracking.entity.UserBillingRate;
import fr.ailegalcase.timetracking.repository.UserBillingRateRepository;
import fr.ailegalcase.timetracking.service.BillingRateService;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingRateServiceTest {

    @Mock private UserBillingRateRepository billingRateRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private AuditLogRepository auditLogRepository;

    private BillingRateService service;

    private User user;
    private Workspace workspace;
    private WorkspaceMember member;

    @BeforeEach
    void setUp() {
        service = new BillingRateService(billingRateRepository, workspaceMemberRepository, currentUserResolver, auditLogRepository);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setStatus("ACTIVE");

        workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);
        member.setMemberRole("MEMBER");
    }

    // U-BR-01 : taux le plus récent retourné correctement parmi plusieurs lignes
    @Test
    void getCurrentRate_returnsLatestRate() {
        UUID workspaceId = workspace.getId();

        UserBillingRate rate = new UserBillingRate();
        rate.setId(UUID.randomUUID());
        rate.setUser(user);
        rate.setWorkspace(workspace);
        rate.setRatePerHour(new BigDecimal("250.00"));
        rate.setEffectiveFrom(LocalDate.now());

        when(billingRateRepository.findCurrentRate(eq(user.getId()), eq(workspaceId)))
                .thenReturn(Optional.of(rate));

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));

        var response = service.getCurrentRate(null, null);

        assertThat(response).isNotNull();
        assertThat(response.ratePerHour()).isEqualByComparingTo(new BigDecimal("250.00"));
    }

    // U-BR-02 : aucun taux configuré → null
    @Test
    void getCurrentRate_noRateConfigured_returnsNull() {
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(billingRateRepository.findCurrentRate(any(), any())).thenReturn(Optional.empty());

        var response = service.getCurrentRate(null, null);

        assertThat(response).isNull();
    }

    // U-BR-03b : setRate sauvegarde un AuditLog BILLING_RATE_UPDATED
    @Test
    void setRate_savesAuditLog() {
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));

        UserBillingRate saved = new UserBillingRate();
        saved.setId(UUID.randomUUID());
        saved.setUser(user);
        saved.setWorkspace(workspace);
        saved.setRatePerHour(new BigDecimal("300.00"));
        saved.setEffectiveFrom(LocalDate.now());
        when(billingRateRepository.save(any())).thenReturn(saved);
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.setRate(new BillingRateRequest(new BigDecimal("300.00")), null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo("BILLING_RATE_UPDATED");
        assertThat(log.getWorkspaceId()).isEqualTo(workspace.getId());
        assertThat(log.getUserId()).isEqualTo(user.getId());
        assertThat(log.getMetadata()).contains("300");
    }

    // U-BR-03 : taux effectif à une date passée
    @Test
    void getRateAtDate_returnsRateInEffectAtDate() {
        LocalDate targetDate = LocalDate.of(2026, 1, 15);

        UserBillingRate rate = new UserBillingRate();
        rate.setId(UUID.randomUUID());
        rate.setRatePerHour(new BigDecimal("200.00"));
        rate.setEffectiveFrom(LocalDate.of(2026, 1, 1));

        when(billingRateRepository.findRateAtDate(eq(user.getId()), eq(workspace.getId()), eq(targetDate)))
                .thenReturn(Optional.of(rate));

        var result = service.getRateAtDate(user.getId(), workspace.getId(), targetDate);

        assertThat(result).isPresent();
        assertThat(result.get().getRatePerHour()).isEqualByComparingTo(new BigDecimal("200.00"));
    }
}
