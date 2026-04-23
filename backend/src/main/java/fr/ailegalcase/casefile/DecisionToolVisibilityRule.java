package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-IA-04-01 : règle déclarative mappant un (legalDomain, country, situation détectée)
 * vers un outil décisionnel à afficher dans une couche donnée (ALWAYS_ON / CONTEXTUAL).
 *
 * Alimentée exclusivement par migrations Liquibase en V1. Pas d'endpoint d'écriture.
 */
@Entity
@Table(name = "decision_tool_visibility_rules")
@Getter
@Setter
public class DecisionToolVisibilityRule {

    public enum Layer {
        ALWAYS_ON,
        CONTEXTUAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "legal_domain", nullable = false, length = 50)
    private String legalDomain;

    @Column(name = "country", length = 20)
    private String country;

    @Column(name = "tool_id", nullable = false, length = 100)
    private String toolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "layer", nullable = false, length = 20)
    private Layer layer;

    @Column(name = "trigger_field", length = 100)
    private String triggerField;

    @Column(name = "trigger_value", length = 100)
    private String triggerValue;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
