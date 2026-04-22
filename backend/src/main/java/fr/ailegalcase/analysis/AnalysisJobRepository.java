package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, UUID> {

    Optional<AnalysisJob> findByCaseFileIdAndJobType(UUID caseFileId, JobType jobType);

    List<AnalysisJob> findByCaseFileId(UUID caseFileId);

    boolean existsByCaseFileIdAndStatusIn(UUID caseFileId, Collection<AnalysisStatus> statuses);

    void deleteByCaseFileIdIn(Collection<UUID> caseFileIds);

    @Query("SELECT COUNT(j) FROM AnalysisJob j WHERE j.status = :status AND j.updatedAt > :since")
    long countByStatusAndUpdatedAtAfter(@Param("status") AnalysisStatus status, @Param("since") Instant since);

    @Modifying
    @Query(value = """
            INSERT INTO analysis_jobs (id, case_file_id, job_type, status, total_items, processed_items, created_at, updated_at)
            VALUES (gen_random_uuid(), :caseFileId, 'DOCUMENT_ANALYSIS', 'PENDING', :totalItems, 0, NOW(), NOW())
            ON CONFLICT (case_file_id, job_type) DO UPDATE
              SET status = 'PENDING', total_items = :totalItems, processed_items = 0, updated_at = NOW()
            """, nativeQuery = true)
    void upsertDocumentAnalysisJob(@Param("caseFileId") UUID caseFileId, @Param("totalItems") int totalItems);

    /**
     * F-147 SF-147-02 : force tous les jobs d'un case file en FAILED (reset manuel
     * super-admin). Ne touche pas les jobs déjà en DONE ou FAILED.
     */
    @Modifying
    @Query("""
            UPDATE AnalysisJob j SET j.status = fr.ailegalcase.analysis.AnalysisStatus.FAILED,
                                     j.errorMessage = :errorMessage, j.updatedAt = :now
            WHERE j.caseFileId = :caseFileId
              AND j.status IN (fr.ailegalcase.analysis.AnalysisStatus.PENDING,
                               fr.ailegalcase.analysis.AnalysisStatus.PROCESSING)
            """)
    int forceFailActiveJobsForCaseFile(@Param("caseFileId") UUID caseFileId,
                                        @Param("errorMessage") String errorMessage,
                                        @Param("now") Instant now);

    /**
     * F-147 SF-147-03 : reset zombies — marque FAILED les jobs PROCESSING/PENDING
     * dont updated_at est plus vieux que le seuil. Tourne via scheduled task.
     */
    @Modifying
    @Query("""
            UPDATE AnalysisJob j SET j.status = fr.ailegalcase.analysis.AnalysisStatus.FAILED,
                                     j.errorMessage = :errorMessage, j.updatedAt = :now
            WHERE j.status IN (fr.ailegalcase.analysis.AnalysisStatus.PENDING,
                               fr.ailegalcase.analysis.AnalysisStatus.PROCESSING)
              AND j.updatedAt < :staleBefore
            """)
    int forceFailZombieJobs(@Param("staleBefore") Instant staleBefore,
                             @Param("errorMessage") String errorMessage,
                             @Param("now") Instant now);
}
