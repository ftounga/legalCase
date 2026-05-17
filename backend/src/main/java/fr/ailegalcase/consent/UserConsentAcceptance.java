package fr.ailegalcase.consent;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * F-240 SF-240-01 : ligne d'audit immuable d'une acceptation contractuelle
 * (CGU, politique de confidentialité, CGV de paiement, téléchargement DPA).
 *
 * <p>Conservée comme preuve RGPD. Pas de soft-delete, pas de mise à jour :
 * une acceptation est figée à sa création. {@code workspaceId} est nullable
 * car {@code SIGNUP_TERMS} est accepté avant la création du workspace.</p>
 */
@Entity
@Table(name = "user_consent_acceptance")
@Getter
@Setter
public class UserConsentAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "consent_type", nullable = false, length = 32)
    private String consentType;

    @Column(name = "version", nullable = false, length = 64)
    private String version;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @Column(name = "acceptance_ip", nullable = false, length = 45)
    private String acceptanceIp;

    @Column(name = "acceptance_user_agent", nullable = false, length = 500)
    private String acceptanceUserAgent;

    @Column(name = "workspace_id")
    private UUID workspaceId;
}
