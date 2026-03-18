package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, UUID> {

    Optional<AnalysisJob> findByCaseFileIdAndJobType(UUID caseFileId, JobType jobType);

    List<AnalysisJob> findByCaseFileId(UUID caseFileId);
}
