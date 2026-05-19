package fr.ailegalcase.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    private String firstName;

    private String lastName;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private boolean isSuperAdmin;

    /**
     * F-248 / SF-248-01 : opt-out des emails non-transactionnels (onboarding, newsletter).
     * Les emails transactionnels restent toujours envoyés. Défaut false (opt-in implicite).
     */
    @Column(name = "marketing_emails_opted_out", nullable = false)
    private boolean marketingEmailsOptedOut;

    /**
     * F-248 / SF-248-01 : token public porté par le lien de désinscription des emails
     * non-transactionnels. Généré paresseusement au premier envoi d'un email
     * non-transactionnel, puis stable. {@code null} tant qu'aucun email de ce type
     * n'a été envoyé.
     */
    @Column(name = "marketing_unsubscribe_token", unique = true)
    private UUID marketingUnsubscribeToken;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onPrePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}
