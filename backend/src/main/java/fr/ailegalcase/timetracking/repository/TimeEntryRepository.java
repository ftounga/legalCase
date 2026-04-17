package fr.ailegalcase.timetracking.repository;

import fr.ailegalcase.timetracking.entity.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, UUID> {

    boolean existsByUser_IdAndStoppedAtIsNull(UUID userId);

    @Query("SELECT te FROM TimeEntry te WHERE te.user.id = :userId AND te.stoppedAt IS NULL AND te.caseFile.deletedAt IS NULL")
    List<TimeEntry> findActiveByUserIdAndCaseFileNotDeleted(@Param("userId") UUID userId);

    @Query("SELECT te FROM TimeEntry te WHERE te.user.id = :userId AND te.stoppedAt IS NULL")
    List<TimeEntry> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(te) > 0 FROM TimeEntry te WHERE te.user.id = :userId AND te.stoppedAt IS NULL AND te.caseFile.deletedAt IS NULL")
    boolean existsActiveByUserIdAndCaseFileNotDeleted(@Param("userId") UUID userId);

    List<TimeEntry> findByCaseFile_IdAndWorkspace_IdOrderByStartedAtDesc(UUID caseFileId, UUID workspaceId);

    @Query("""
            SELECT te FROM TimeEntry te
            WHERE te.workspace.id = :workspaceId
              AND te.stoppedAt IS NOT NULL
              AND te.startedAt >= :monthStart
              AND te.startedAt < :monthEnd
            ORDER BY te.startedAt DESC
            """)
    List<TimeEntry> findCompletedByWorkspaceAndMonth(
            @Param("workspaceId") UUID workspaceId,
            @Param("monthStart") Instant monthStart,
            @Param("monthEnd") Instant monthEnd);

    @Query("""
            SELECT te FROM TimeEntry te
            WHERE te.workspace.id = :workspaceId
              AND te.user.id = :userId
              AND te.stoppedAt IS NOT NULL
              AND te.startedAt >= :monthStart
              AND te.startedAt < :monthEnd
            ORDER BY te.startedAt DESC
            """)
    List<TimeEntry> findCompletedByWorkspaceUserAndMonth(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId,
            @Param("monthStart") Instant monthStart,
            @Param("monthEnd") Instant monthEnd);

    Optional<TimeEntry> findByIdAndWorkspace_Id(UUID id, UUID workspaceId);

    @Query("SELECT te FROM TimeEntry te WHERE te.stoppedAt IS NULL AND te.startedAt < :cutoff")
    List<TimeEntry> findStaleTimers(@Param("cutoff") Instant cutoff);

    @Query("SELECT te FROM TimeEntry te WHERE te.caseFile.id = :caseFileId AND te.stoppedAt IS NULL")
    List<TimeEntry> findActiveByCaseFileId(@Param("caseFileId") UUID caseFileId);
}
