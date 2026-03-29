package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_sends")
@Getter
@Setter
public class EmailSend {

    public enum EmailType {
        ONBOARDING_WELCOME,
        ONBOARDING_TIP_ANALYSIS,
        ONBOARDING_TIP_SHARE,
        ONBOARDING_BEFORE_EXPIRY,
        ONBOARDING_EXPIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false, length = 100)
    private EmailType emailType;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
}
