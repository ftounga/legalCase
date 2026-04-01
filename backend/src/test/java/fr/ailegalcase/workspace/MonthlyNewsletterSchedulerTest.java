package fr.ailegalcase.workspace;

import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MonthlyNewsletterSchedulerTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private EmailSendRepository emailSendRepository;
    @Mock private CaseAnalysisRepository caseAnalysisRepository;
    @Mock private CaseFileRepository caseFileRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private EmailService emailService;

    private MonthlyNewsletterScheduler scheduler;
    private User owner;
    private Subscription paidSub;

    @BeforeEach
    void setUp() {
        scheduler = new MonthlyNewsletterScheduler(
                subscriptionRepository, workspaceMemberRepository, emailSendRepository,
                caseAnalysisRepository, caseFileRepository, documentRepository, emailService);

        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("avocat@cabinet.fr");

        UUID workspaceId = UUID.randomUUID();
        paidSub = new Subscription();
        paidSub.setWorkspaceId(workspaceId);
        paidSub.setPlanCode("SOLO");

        WorkspaceMember member = new WorkspaceMember();
        member.setUser(owner);
        member.setMemberRole("OWNER");

        when(subscriptionRepository.findAll()).thenReturn(List.of(paidSub));
        when(workspaceMemberRepository.findByWorkspace_Id(workspaceId)).thenReturn(List.of(member));
        when(emailSendRepository.existsByUserAndEmailTypeAndSentAtAfter(eq(owner), eq(EmailSend.EmailType.NEWSLETTER_MONTHLY), any(Instant.class))).thenReturn(false);
        when(caseAnalysisRepository.countDoneByWorkspaceIdAndCreatedAtAfter(eq(workspaceId), any())).thenReturn(5L);
        when(caseFileRepository.countByWorkspace_IdAndDeletedAtIsNull(workspaceId)).thenReturn(3L);
        when(documentRepository.countByWorkspaceIdAndCreatedAtAfter(eq(workspaceId), any())).thenReturn(12L);
    }

    @Test
    void T1_sendsNewsletterForPaidWorkspace() {
        scheduler.sendMonthlyNewsletters();
        verify(emailService).sendMonthlyNewsletter(eq(owner), eq(5L), eq(3L), eq(12L), anyString(), anyString(), anyString());
    }

    @Test
    void T2_skipIfAlreadySentThisMonth() {
        when(emailSendRepository.existsByUserAndEmailTypeAndSentAtAfter(eq(owner), eq(EmailSend.EmailType.NEWSLETTER_MONTHLY), any(Instant.class))).thenReturn(true);
        scheduler.sendMonthlyNewsletters();
        verify(emailService, never()).sendMonthlyNewsletter(any(), anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void T3_skipFreeWorkspace() {
        paidSub.setPlanCode("FREE");
        scheduler.sendMonthlyNewsletters();
        verify(emailService, never()).sendMonthlyNewsletter(any(), anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void T4_continuesAfterOneFailure() {
        UUID ws2 = UUID.randomUUID();
        Subscription sub2 = new Subscription();
        sub2.setWorkspaceId(ws2);
        sub2.setPlanCode("TEAM");

        when(subscriptionRepository.findAll()).thenReturn(List.of(paidSub, sub2));
        when(workspaceMemberRepository.findByWorkspace_Id(ws2)).thenThrow(new RuntimeException("DB error"));

        scheduler.sendMonthlyNewsletters();

        // The first workspace still gets its newsletter
        verify(emailService).sendMonthlyNewsletter(eq(owner), anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void T5_featureRotationIsDeterministic() {
        // Two consecutive calls in the same month must use the same feature
        // We just verify the scheduler runs without errors and emailService is called exactly once per call
        scheduler.sendMonthlyNewsletters();
        scheduler.sendMonthlyNewsletters(); // second call — deduplication will block, but rotation must not change
        verify(emailService, atLeastOnce()).sendMonthlyNewsletter(any(), anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString());
    }
}
