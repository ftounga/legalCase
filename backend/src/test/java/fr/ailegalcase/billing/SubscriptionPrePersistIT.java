package fr.ailegalcase.billing;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-251 SF-251-02 — IT vérifiant que le hook {@code @PrePersist} sur
 * {@link Subscription} est bien câblé par JPA et applique le fallback
 * {@code expiresAt} lors d'un {@code save} direct via repository.
 */
@DataJpaTest
class SubscriptionPrePersistIT {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveFreeWithoutExpiresAt_persistsWithExpiresAtSet() {
        UUID workspaceId = newWorkspaceWithOwner("free-without@example.com");
        Instant startedAt = Instant.parse("2026-05-01T10:00:00Z");

        Subscription sub = new Subscription();
        sub.setWorkspaceId(workspaceId);
        sub.setPlanCode("FREE");
        sub.setStatus("ACTIVE");
        sub.setStartedAt(startedAt);
        // expiresAt délibérément null — simule un appel bypass IHM
        assertThat(sub.getExpiresAt()).isNull();

        subscriptionRepository.save(sub);
        entityManager.flush();
        entityManager.clear();

        Subscription reloaded = subscriptionRepository.findByWorkspaceId(workspaceId).orElseThrow();
        assertThat(reloaded.getExpiresAt())
                .as("Le hook @PrePersist doit avoir fixé expiresAt = startedAt + 14j")
                .isEqualTo(startedAt.plus(14, ChronoUnit.DAYS));
    }

    @Test
    void saveFreeWithExpiresAt_persistsValueAsIs() {
        UUID workspaceId = newWorkspaceWithOwner("free-explicit@example.com");
        Instant startedAt = Instant.parse("2026-05-01T10:00:00Z");
        Instant explicitExpiresAt = Instant.parse("2027-01-01T10:00:00Z");

        Subscription sub = new Subscription();
        sub.setWorkspaceId(workspaceId);
        sub.setPlanCode("FREE");
        sub.setStatus("ACTIVE");
        sub.setStartedAt(startedAt);
        sub.setExpiresAt(explicitExpiresAt);

        subscriptionRepository.save(sub);
        entityManager.flush();
        entityManager.clear();

        Subscription reloaded = subscriptionRepository.findByWorkspaceId(workspaceId).orElseThrow();
        assertThat(reloaded.getExpiresAt())
                .as("Hook défensif : ne pas écraser la valeur explicite")
                .isEqualTo(explicitExpiresAt);
    }

    @Test
    void saveSoloWithoutExpiresAt_persistsWithNullExpiresAt() {
        UUID workspaceId = newWorkspaceWithOwner("solo-without@example.com");
        Instant startedAt = Instant.parse("2026-05-01T10:00:00Z");

        Subscription sub = new Subscription();
        sub.setWorkspaceId(workspaceId);
        sub.setPlanCode("SOLO");
        sub.setStatus("ACTIVE");
        sub.setStartedAt(startedAt);
        // expiresAt null — légitime pour plan payant (cycle Stripe)

        subscriptionRepository.save(sub);
        entityManager.flush();
        entityManager.clear();

        Subscription reloaded = subscriptionRepository.findByWorkspaceId(workspaceId).orElseThrow();
        assertThat(reloaded.getExpiresAt())
                .as("Plan payant → hors scope F-251, expiresAt reste null")
                .isNull();
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
}
