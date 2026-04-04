package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CaseDeadlineRepository extends JpaRepository<CaseDeadline, UUID> {

    List<CaseDeadline> findByCaseFileIdOrderByDueDateAsc(UUID caseFileId);

    List<CaseDeadline> findByDueDateInAndCaseFileDeletedAtIsNull(Collection<LocalDate> dates);

    List<CaseDeadline> findByAlertThresholdsIsNotNullAndCaseFileDeletedAtIsNull();

    @Query("SELECT d FROM CaseDeadline d WHERE d.caseFile.workspace.id = :workspaceId AND d.caseFile.deletedAt IS NULL AND d.dueDate <= :cutoff ORDER BY d.dueDate ASC")
    List<CaseDeadline> findUrgentByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("cutoff") LocalDate cutoff);
}
