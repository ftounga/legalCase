package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CaseDeadlineRepository extends JpaRepository<CaseDeadline, UUID> {

    List<CaseDeadline> findByCaseFileIdOrderByDueDateAsc(UUID caseFileId);

    List<CaseDeadline> findByDueDateInAndCaseFileDeletedAtIsNull(Collection<LocalDate> dates);
}
