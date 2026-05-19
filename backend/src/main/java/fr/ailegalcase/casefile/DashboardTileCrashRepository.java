package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * F-180 SF-180-01 — accès aux crashes runtime de mappers {@link DashboardTile}.
 */
public interface DashboardTileCrashRepository extends JpaRepository<DashboardTileCrash, UUID> {

    /** Crashes survenus depuis {@code since} (fenêtre 168h pour le panel 🔴). */
    List<DashboardTileCrash> findByOccurredAtAfter(Instant since);

    /** Purge de rétention — supprime les crashes plus anciens que {@code cutoff} (30j). */
    @Modifying
    @Query("DELETE FROM DashboardTileCrash c WHERE c.occurredAt < :cutoff")
    int deleteByOccurredAtBefore(@Param("cutoff") Instant cutoff);
}
