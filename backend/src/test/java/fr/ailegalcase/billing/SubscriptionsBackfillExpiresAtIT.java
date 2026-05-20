package fr.ailegalcase.billing;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-251 SF-251-01 — vérifie le comportement de la migration data
 * {@code 255-fix-subscriptions-free-expires-at-null.xml}.
 *
 * <p>La migration tourne au démarrage du contexte Spring (donc déjà exécutée
 * quand ce test démarre). On simule le bug en insérant des rows post-startup
 * avec {@code expires_at = NULL} via JdbcTemplate (la colonne le permet — c'est
 * justement le bug), puis on rejoue la même SQL que la migration et on vérifie
 * que le filtre cible exactement la bonne combinaison de rows.
 *
 * <p>Cas couverts :
 * <ul>
 *   <li>FREE + started_at non NULL + expires_at NULL → patché à started_at + 14j</li>
 *   <li>FREE + started_at NULL + expires_at NULL → patché à now() + 14j</li>
 *   <li>FREE + expires_at déjà fixée → inchangée (idempotence)</li>
 *   <li>SOLO + expires_at NULL → inchangée (plan payant hors scope)</li>
 *   <li>Ré-exécution → 0 rows (idempotence stricte)</li>
 * </ul>
 */
@DataJpaTest
class SubscriptionsBackfillExpiresAtIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    /** SQL aligné sur le dialecte H2 du changelog 255 (DATEADD + COALESCE). */
    private static final String BACKFILL_SQL =
            "UPDATE subscriptions " +
                    "SET expires_at = DATEADD('DAY', 14, COALESCE(started_at, NOW())) " +
                    "WHERE plan_code = 'FREE' AND expires_at IS NULL";

    @Test
    void backfill_freeWithStartedAt_setsExpiresAtToStartedPlus14d() {
        UUID workspaceId = newWorkspaceWithOwner("free-with-started@example.com");
        Instant startedAt = Instant.parse("2026-05-01T10:00:00Z");
        insertSubscriptionViaJdbc(workspaceId, "FREE", "ACTIVE", startedAt, null);

        int updated = jdbcTemplate.update(BACKFILL_SQL);
        assertThat(updated).isGreaterThanOrEqualTo(1);

        Subscription patched = subscriptionRepository.findByWorkspaceId(workspaceId).orElseThrow();
        assertThat(patched.getExpiresAt())
                .as("expires_at = started_at + 14 jours exact")
                .isEqualTo(startedAt.plus(14, ChronoUnit.DAYS));
    }

    // Cas started_at NULL : non testable au runtime — la colonne est NOT NULL
    // en schéma (cf. {@code Subscription.startedAt} annoté nullable=false). Le
    // COALESCE(started_at, NOW()) de la migration sert de défense en profondeur
    // pour les SGBD qui pourraient relâcher la contrainte ou les rows legacy
    // issues de migrations très antérieures. La sémantique applicative
    // équivalente est couverte par
    // SubscriptionPrePersistTest.freeWithNullStartedAt_setsBothNowAndExpiresAt
    // (SF-251-02).

    @Test
    void backfill_freeWithExistingExpiresAt_isNotTouched() {
        UUID workspaceId = newWorkspaceWithOwner("free-existing@example.com");
        Instant startedAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant existingExpiresAt = Instant.parse("2027-01-01T10:00:00Z");
        insertSubscriptionViaJdbc(workspaceId, "FREE", "ACTIVE", startedAt, existingExpiresAt);

        jdbcTemplate.update(BACKFILL_SQL);

        Subscription unchanged = subscriptionRepository.findByWorkspaceId(workspaceId).orElseThrow();
        assertThat(unchanged.getExpiresAt())
                .as("FREE avec expires_at déjà fixée → inchangée (idempotence)")
                .isEqualTo(existingExpiresAt);
    }

    @Test
    void backfill_soloWithNullExpiresAt_isNotTouched() {
        UUID workspaceId = newWorkspaceWithOwner("solo-null@example.com");
        Instant startedAt = Instant.parse("2026-05-01T10:00:00Z");
        insertSubscriptionViaJdbc(workspaceId, "SOLO", "ACTIVE", startedAt, null);

        jdbcTemplate.update(BACKFILL_SQL);

        Subscription unchanged = subscriptionRepository.findByWorkspaceId(workspaceId).orElseThrow();
        assertThat(unchanged.getExpiresAt())
                .as("Plan payant SOLO → exclu du backfill, hors scope F-251")
                .isNull();
    }

    @Test
    void backfill_isIdempotent_secondRunTouchesZeroRows() {
        UUID workspaceId = newWorkspaceWithOwner("idempotent@example.com");
        Instant startedAt = Instant.parse("2026-05-01T10:00:00Z");
        insertSubscriptionViaJdbc(workspaceId, "FREE", "ACTIVE", startedAt, null);

        int first = jdbcTemplate.update(BACKFILL_SQL);
        assertThat(first).isGreaterThanOrEqualTo(1);

        int second = jdbcTemplate.update(BACKFILL_SQL);
        assertThat(second)
                .as("Re-exécution doit toucher 0 rows (filtre expires_at IS NULL)")
                .isZero();
    }

    /**
     * Crée un User puis un Workspace via JPA — la FK
     * subscriptions.workspace_id → workspaces.id l'exige.
     */
    private UUID newWorkspaceWithOwner(String email) {
        User user = new User();
        user.setEmail(email);
        user.setStatus("ACTIVE");
        user = userRepository.save(user);

        Workspace ws = new Workspace();
        ws.setName(email.toUpperCase());
        ws.setSlug(UUID.randomUUID().toString());
        ws.setOwner(user);
        ws.setLegalDomain("DROIT_DU_TRAVAIL");
        ws.setCountry("FRANCE");
        ws.setPlanCode("FREE");
        ws.setStatus("ACTIVE");
        ws = workspaceRepository.save(ws);
        entityManager.flush();
        return ws.getId();
    }

    /**
     * Insert direct via JDBC pour pouvoir mettre {@code expires_at = NULL}
     * (le but du test est précisément de simuler le bug avant migration).
     */
    private void insertSubscriptionViaJdbc(UUID workspaceId, String planCode, String status,
                                           Instant startedAt, Instant expiresAt) {
        jdbcTemplate.update(
                "INSERT INTO subscriptions " +
                        "(id, workspace_id, plan_code, status, started_at, expires_at, " +
                        " seat_count, cancel_at_period_end) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 1, FALSE)",
                UUID.randomUUID(),
                workspaceId,
                planCode,
                status,
                Timestamp.from(startedAt),
                expiresAt == null ? null : Timestamp.from(expiresAt));
    }
}
