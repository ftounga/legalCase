package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, UUID> {

    Optional<AnalysisJob> findByCaseFileIdAndJobType(UUID caseFileId, JobType jobType);

    List<AnalysisJob> findByCaseFileId(UUID caseFileId);

    boolean existsByCaseFileIdAndStatusIn(UUID caseFileId, Collection<AnalysisStatus> statuses);

    void deleteByCaseFileIdIn(Collection<UUID> caseFileIds);

    @Modifying
    @Query(value = """
            INSERT INTO analysis_jobs (id, case_file_id, job_type, status, total_items, processed_items, created_at, updated_at)
            VALUES (gen_random_uuid(), :caseFileId, 'DOCUMENT_ANALYSIS', 'PENDING', :totalItems, 0, NOW(), NOW())
            ON CONFLICT (case_file_id, job_type) DO UPDATE
              SET status = 'PENDING', total_items = :totalItems, processed_items = 0, updated_at = NOW()
            """, nativeQuery = true)
    void upsertDocumentAnalysisJob(@Param("caseFileId") UUID caseFileId, @Param("totalItems") int totalItems);
}
