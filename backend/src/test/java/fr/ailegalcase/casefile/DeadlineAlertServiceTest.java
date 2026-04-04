package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.workspace.EmailService;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadlineAlertServiceTest {

    @Mock private CaseDeadlineRepository deadlineRepository;
    @Mock private DeadlineAlertSendRepository alertSendRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private EmailService emailService;
    @InjectMocks private DeadlineAlertService service;

    private Workspace workspace;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@example.com");
        owner.setStatus("ACTIVE");

        workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setName("TEST");
        workspace.setOwner(owner);

        caseFile = new CaseFile();
        caseFile.setId(UUID.randomUUID());
        caseFile.setTitle("Dossier Dupont");
        caseFile.setWorkspace(workspace);
    }

    private CaseDeadline makeDeadline(LocalDate dueDate) {
        CaseDeadline d = new CaseDeadline();
        d.setId(UUID.randomUUID());
        d.setCaseFile(caseFile);
        d.setLabel("Délai de prescription");
        d.setDueDate(dueDate);
        return d;
    }

    private WorkspaceMember makeMember(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setStatus("ACTIVE");
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("MEMBER");
        return member;
    }

    // U-01 : délais à J-15 et J-7 → emails envoyés
    @Test
    void sendDailyAlerts_deadlinesAtJ15AndJ7_sendsEmails() {
        LocalDate today = LocalDate.now();
        CaseDeadline d15 = makeDeadline(today.plusDays(15));
        CaseDeadline d7 = makeDeadline(today.plusDays(7));
        WorkspaceMember member = makeMember("avocat@example.com");

        when(deadlineRepository.findByDueDateInAndCaseFileDeletedAtIsNull(anyCollection()))
                .thenReturn(List.of(d15, d7));
        when(workspaceMemberRepository.findByWorkspace_Id(workspace.getId()))
                .thenReturn(List.of(member));

        service.sendDailyAlerts();

        verify(emailService, times(2)).sendDeadlineAlert(
                eq("avocat@example.com"), eq("Dossier Dupont"), eq(caseFile.getId()),
                eq("Délai de prescription"), anyString(), anyInt());
    }

    // U-02 : aucun délai → aucun email
    @Test
    void sendDailyAlerts_noDeadlines_sendsNoEmail() {
        when(deadlineRepository.findByDueDateInAndCaseFileDeletedAtIsNull(anyCollection()))
                .thenReturn(List.of());

        service.sendDailyAlerts();

        verifyNoInteractions(emailService);
    }

    // U-03 : exception mail pour un membre → autres membres traités
    @Test
    void sendDailyAlerts_emailFailsForOneMember_continuesForOthers() {
        LocalDate today = LocalDate.now();
        CaseDeadline d7 = makeDeadline(today.plusDays(7));
        WorkspaceMember m1 = makeMember("avocat1@example.com");
        WorkspaceMember m2 = makeMember("avocat2@example.com");

        when(deadlineRepository.findByDueDateInAndCaseFileDeletedAtIsNull(anyCollection()))
                .thenReturn(List.of(d7));
        when(workspaceMemberRepository.findByWorkspace_Id(workspace.getId()))
                .thenReturn(List.of(m1, m2));
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService).sendDeadlineAlert(eq("avocat1@example.com"), any(), any(), any(), any(), anyInt());

        // ne doit pas lever d'exception
        service.sendDailyAlerts();

        verify(emailService).sendDeadlineAlert(eq("avocat2@example.com"), any(), any(), any(), any(), anyInt());
    }

    // U-MT-01 : délai multi-threshold à J-30 → email envoyé, send enregistré
    @Test
    void multiThreshold_deadlineAtMatchingThreshold_sendsEmailAndRecords() {
        LocalDate today = LocalDate.now();
        CaseDeadline deadline = makeDeadline(today.plusDays(30));
        deadline.setAlertThresholds("90,30,7");
        WorkspaceMember member = makeMember("avocat@example.com");

        when(deadlineRepository.findByAlertThresholdsIsNotNullAndCaseFileDeletedAtIsNull())
                .thenReturn(List.of(deadline));
        when(alertSendRepository.existsByDeadlineIdAndThresholdDays(deadline.getId(), 30))
                .thenReturn(false);
        when(workspaceMemberRepository.findByWorkspace_Id(workspace.getId()))
                .thenReturn(List.of(member));

        service.sendMultiThresholdAlerts(today);

        verify(emailService).sendDeadlineAlert(
                eq("avocat@example.com"), eq("Dossier Dupont"), eq(caseFile.getId()),
                eq("Délai de prescription"), anyString(), eq(30));
        ArgumentCaptor<DeadlineAlertSend> sendCaptor = ArgumentCaptor.forClass(DeadlineAlertSend.class);
        verify(alertSendRepository).save(sendCaptor.capture());
        assertThat(sendCaptor.getValue().getThresholdDays()).isEqualTo(30);
    }

    // U-MT-02 : threshold déjà envoyé → pas d'email
    @Test
    void multiThreshold_alreadySent_skipsEmail() {
        LocalDate today = LocalDate.now();
        CaseDeadline deadline = makeDeadline(today.plusDays(7));
        deadline.setAlertThresholds("90,30,7");

        when(deadlineRepository.findByAlertThresholdsIsNotNullAndCaseFileDeletedAtIsNull())
                .thenReturn(List.of(deadline));
        when(alertSendRepository.existsByDeadlineIdAndThresholdDays(deadline.getId(), 7))
                .thenReturn(true);

        service.sendMultiThresholdAlerts(today);

        verifyNoInteractions(emailService);
        verify(alertSendRepository, never()).save(any());
    }

    // U-MT-03 : délai multi-threshold non atteint (J-45) → pas d'email
    @Test
    void multiThreshold_thresholdNotReached_skipsEmail() {
        LocalDate today = LocalDate.now();
        CaseDeadline deadline = makeDeadline(today.plusDays(45));
        deadline.setAlertThresholds("90,30,7");

        when(deadlineRepository.findByAlertThresholdsIsNotNullAndCaseFileDeletedAtIsNull())
                .thenReturn(List.of(deadline));

        service.sendMultiThresholdAlerts(today);

        verifyNoInteractions(emailService);
        verify(alertSendRepository, never()).save(any());
    }

    // U-MT-04 : standard path ignore les deadlines avec alertThresholds
    @Test
    void standardAlerts_skipsDeadlineWithAlertThresholds() {
        LocalDate today = LocalDate.now();
        CaseDeadline withThresholds = makeDeadline(today.plusDays(15));
        withThresholds.setAlertThresholds("90,30,7");

        when(deadlineRepository.findByDueDateInAndCaseFileDeletedAtIsNull(anyCollection()))
                .thenReturn(List.of(withThresholds));

        service.sendStandardAlerts(today);

        verifyNoInteractions(emailService);
    }

    // U-MT-05 : parseThresholds — cas variés
    @Test
    void parseThresholds_variousCases() {
        assertThat(service.parseThresholds("90,30,7")).containsExactly(90, 30, 7);
        assertThat(service.parseThresholds("15")).containsExactly(15);
        assertThat(service.parseThresholds(null)).isEmpty();
        assertThat(service.parseThresholds("  ")).isEmpty();
        assertThat(service.parseThresholds("7, 30, 90")).containsExactly(7, 30, 90);
    }

    // U-04 : workspace avec 2 membres → 2 emails envoyés
    @Test
    void sendDailyAlerts_twoMembers_sendsTwoEmails() {
        LocalDate today = LocalDate.now();
        CaseDeadline d15 = makeDeadline(today.plusDays(15));
        WorkspaceMember m1 = makeMember("avocat1@example.com");
        WorkspaceMember m2 = makeMember("avocat2@example.com");

        when(deadlineRepository.findByDueDateInAndCaseFileDeletedAtIsNull(anyCollection()))
                .thenReturn(List.of(d15));
        when(workspaceMemberRepository.findByWorkspace_Id(workspace.getId()))
                .thenReturn(List.of(m1, m2));

        service.sendDailyAlerts();

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(2)).sendDeadlineAlert(
                emailCaptor.capture(), any(), any(), any(), any(), eq(15));
        assertThat(emailCaptor.getAllValues())
                .containsExactlyInAnyOrder("avocat1@example.com", "avocat2@example.com");
    }
}
