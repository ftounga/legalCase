package fr.ailegalcase.referential;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferentialAlertRepository extends JpaRepository<ReferentialAlert, UUID> {

    long countByStatus(ReferentialAlert.Status status);

    Optional<ReferentialAlert> findByEntry_IdAndStatus(UUID entryId, ReferentialAlert.Status status);

    @Query("""
            SELECT a FROM ReferentialAlert a
            WHERE a.status = 'PENDING'
              AND a.reminderSentAt IS NULL
              AND a.detectedAt < :threshold
            """)
    List<ReferentialAlert> findPendingWithoutReminderBefore(@Param("threshold") Instant threshold);
}
