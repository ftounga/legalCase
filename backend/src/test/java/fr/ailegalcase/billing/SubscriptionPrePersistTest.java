package fr.ailegalcase.billing;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-251 SF-251-02 — tests unitaires du garde-fou
 * {@link Subscription#applyTrialExpiresAtFallback()}.
 *
 * <p>Les tests appellent la méthode {@code @PrePersist} directement (visibilité
 * package) pour découpler du conteneur JPA. L'IT
 * {@code SubscriptionPrePersistIT} couvre le câblage JPA réel.
 */
class SubscriptionPrePersistTest {

    @Test
    void freeWithStartedAt_nullExpiresAt_setsExpiresAtToStartedPlus14d() {
        Instant startedAt = Instant.parse("2026-05-01T10:00:00Z");
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setStatus("ACTIVE");
        sub.setStartedAt(startedAt);
        // expiresAt délibérément null

        sub.applyTrialExpiresAtFallback();

        assertThat(sub.getStartedAt()).isEqualTo(startedAt);
        assertThat(sub.getExpiresAt()).isEqualTo(startedAt.plus(14, ChronoUnit.DAYS));
    }

    @Test
    void freeWithNullStartedAt_setsBothNowAndExpiresAt() {
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setStatus("ACTIVE");
        // startedAt et expiresAt délibérément null
        Instant before = Instant.now();

        sub.applyTrialExpiresAtFallback();

        Instant after = Instant.now();
        assertThat(sub.getStartedAt())
                .as("startedAt fixé à now() en fallback")
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
        assertThat(sub.getExpiresAt())
                .as("expiresAt = startedAt + 14 jours")
                .isEqualTo(sub.getStartedAt().plus(14, ChronoUnit.DAYS));
    }

    @Test
    void freeWithFournisExpiresAt_isNoOp() {
        Instant startedAt = Instant.parse("2026-05-01T10:00:00Z");
        Instant existingExpiresAt = Instant.parse("2027-01-01T10:00:00Z");
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setStatus("ACTIVE");
        sub.setStartedAt(startedAt);
        sub.setExpiresAt(existingExpiresAt);

        sub.applyTrialExpiresAtFallback();

        assertThat(sub.getStartedAt()).as("startedAt inchangé").isEqualTo(startedAt);
        assertThat(sub.getExpiresAt())
                .as("expiresAt fourni → pas d'écrasement")
                .isEqualTo(existingExpiresAt);
    }

    @Test
    void soloWithNullExpiresAt_isNoOp() {
        Instant startedAt = Instant.parse("2026-05-01T10:00:00Z");
        Subscription sub = new Subscription();
        sub.setPlanCode("SOLO");
        sub.setStatus("ACTIVE");
        sub.setStartedAt(startedAt);
        // expiresAt null — légitime pour plan payant (géré par Stripe webhook)

        sub.applyTrialExpiresAtFallback();

        assertThat(sub.getStartedAt()).isEqualTo(startedAt);
        assertThat(sub.getExpiresAt())
                .as("Plan payant → hors scope F-251, expiresAt reste null")
                .isNull();
    }

    @Test
    void teamWithNullExpiresAt_isNoOp() {
        Subscription sub = new Subscription();
        sub.setPlanCode("TEAM");
        sub.setStatus("ACTIVE");
        sub.setStartedAt(Instant.now());

        sub.applyTrialExpiresAtFallback();

        assertThat(sub.getExpiresAt()).isNull();
    }

    @Test
    void proWithNullExpiresAt_isNoOp() {
        Subscription sub = new Subscription();
        sub.setPlanCode("PRO");
        sub.setStatus("ACTIVE");
        sub.setStartedAt(Instant.now());

        sub.applyTrialExpiresAtFallback();

        assertThat(sub.getExpiresAt()).isNull();
    }
}
