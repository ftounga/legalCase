package fr.ailegalcase.workspace;

import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.contact.ContactRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private EmailSendRepository emailSendRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@example.com");
        user.setFirstName("Alice");
        user.setStatus("ACTIVE");
    }

    private EmailService enabledService() {
        return new EmailService(mailSender, emailSendRepository, true, "http://localhost:4200", "noreply@test.com", "team@test.com");
    }

    private EmailService disabledService() {
        return new EmailService(mailSender, emailSendRepository, false, "http://localhost:4200", "noreply@test.com", "team@test.com");
    }

    // U-01 : mail activé → JavaMailSender.send() appelé avec les bons paramètres
    @Test
    void sendInvitation_whenEnabled_sendsEmail() {
        enabledService().sendInvitation("invitee@example.com", "Mon Cabinet", "tok-123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactly("invitee@example.com");
        assertThat(msg.getSubject()).contains("Mon Cabinet");
        assertThat(msg.getText()).contains("http://localhost:4200/invite?token=tok-123");
    }

    // U-02 : mail désactivé → JavaMailSender.send() non appelé
    @Test
    void sendInvitation_whenDisabled_doesNotSend() {
        disabledService().sendInvitation("invitee@example.com", "Mon Cabinet", "tok-123");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // U-03 : SMTP échoue → pas d'exception propagée (fail-open)
    @Test
    void sendInvitation_smtpFailure_doesNotThrow() {
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatNoException().isThrownBy(
                () -> enabledService().sendInvitation("invitee@example.com", "Mon Cabinet", "tok-123"));
    }

    // U-04 : sendAnalysisDone activé → message envoyé avec bon sujet et lien
    @Test
    void sendAnalysisDone_whenEnabled_sendsEmail() {
        UUID caseFileId = UUID.randomUUID();

        enabledService().sendAnalysisDone("avocat@example.com", caseFileId, "Dossier Dupont", JobType.CASE_ANALYSIS);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactly("avocat@example.com");
        assertThat(msg.getSubject()).contains("Dossier Dupont");
        assertThat(msg.getText()).contains("http://localhost:4200/case-files/" + caseFileId);
        assertThat(msg.getText()).contains("standard");
    }

    // U-05 : sendAnalysisDone désactivé → aucun envoi
    @Test
    void sendAnalysisDone_whenDisabled_doesNotSend() {
        disabledService().sendAnalysisDone("avocat@example.com", UUID.randomUUID(), "Dossier Dupont", JobType.CASE_ANALYSIS);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // U-06 : sendAnalysisDone ENRICHED_ANALYSIS → libellé "enrichie"
    @Test
    void sendAnalysisDone_enrichedType_usesEnrichedLabel() {
        enabledService().sendAnalysisDone("avocat@example.com", UUID.randomUUID(), "Dossier Dupont", JobType.ENRICHED_ANALYSIS);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains("enrichie");
    }

    // ── Onboarding ────────────────────────────────────────────────────────────

    // T6 : sendOnboardingWelcome → sujet bienvenue, corps contient frontendUrl
    @Test
    void sendOnboardingWelcome_sendsCorrectEmail() {
        when(emailSendRepository.existsByUserAndEmailType(user, EmailSend.EmailType.ONBOARDING_WELCOME)).thenReturn(false);

        enabledService().sendOnboardingWelcome(user);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactly("alice@example.com");
        assertThat(msg.getSubject()).containsIgnoringCase("bienvenue");
        assertThat(msg.getText()).contains("http://localhost:4200");
        assertThat(msg.getText()).contains("Alice");
    }

    // T7 : sendOnboardingTipAnalysis → sujet contient "analyse", déduplication skip si déjà envoyé
    @Test
    void sendOnboardingTipAnalysis_skipsIfAlreadySent() {
        when(emailSendRepository.existsByUserAndEmailType(user, EmailSend.EmailType.ONBOARDING_TIP_ANALYSIS)).thenReturn(true);

        enabledService().sendOnboardingTipAnalysis(user);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // T8 : sendOnboardingTipShare → corps contient "Partager"
    @Test
    void sendOnboardingTipShare_sendsCorrectEmail() {
        when(emailSendRepository.existsByUserAndEmailType(user, EmailSend.EmailType.ONBOARDING_TIP_SHARE)).thenReturn(false);

        enabledService().sendOnboardingTipShare(user);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains("Partager");
    }

    // T9 : sendOnboardingBeforeExpiry → corps contient "3 jours"
    @Test
    void sendOnboardingBeforeExpiry_sendsCorrectEmail() {
        when(emailSendRepository.existsByUserAndEmailType(user, EmailSend.EmailType.ONBOARDING_BEFORE_EXPIRY)).thenReturn(false);

        enabledService().sendOnboardingBeforeExpiry(user);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains("3 jours");
    }

    // ── Contact ───────────────────────────────────────────────────────────────

    // U-C1 : sendContactToTeam mail disabled → aucun envoi
    @Test
    void sendContactToTeam_whenDisabled_doesNotSend() {
        ContactRequest req = new ContactRequest("Alice", "alice@example.com", null, "Démo", "Bonjour");
        disabledService().sendContactToTeam(req);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // U-C2 : sendContactConfirmation mail disabled → aucun envoi
    @Test
    void sendContactConfirmation_whenDisabled_doesNotSend() {
        ContactRequest req = new ContactRequest("Alice", "alice@example.com", null, "Démo", "Bonjour");
        disabledService().sendContactConfirmation(req);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // T10 : sendOnboardingExpired → corps contient "terminé"
    @Test
    void sendOnboardingExpired_sendsCorrectEmail() {
        when(emailSendRepository.existsByUserAndEmailType(user, EmailSend.EmailType.ONBOARDING_EXPIRED)).thenReturn(false);

        enabledService().sendOnboardingExpired(user);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains("terminé");
    }
}
