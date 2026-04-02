package fr.ailegalcase.timetracking.repository;

import fr.ailegalcase.timetracking.entity.UserBillingRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface UserBillingRateRepository extends JpaRepository<UserBillingRate, UUID> {

    @Query("""
            SELECT ubr FROM UserBillingRate ubr
            WHERE ubr.user.id = :userId
              AND ubr.workspace.id = :workspaceId
            ORDER BY ubr.effectiveFrom DESC
            LIMIT 1
            """)
    Optional<UserBillingRate> findCurrentRate(
            @Param("userId") UUID userId,
            @Param("workspaceId") UUID workspaceId);

    @Query("""
            SELECT ubr FROM UserBillingRate ubr
            WHERE ubr.user.id = :userId
              AND ubr.workspace.id = :workspaceId
              AND ubr.effectiveFrom <= :date
            ORDER BY ubr.effectiveFrom DESC
            LIMIT 1
            """)
    Optional<UserBillingRate> findRateAtDate(
            @Param("userId") UUID userId,
            @Param("workspaceId") UUID workspaceId,
            @Param("date") LocalDate date);
}
