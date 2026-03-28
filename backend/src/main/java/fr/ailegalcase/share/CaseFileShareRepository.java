package fr.ailegalcase.share;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaseFileShareRepository extends JpaRepository<CaseFileShare, UUID> {

    Optional<CaseFileShare> findByToken(String token);

    @Query("""
        SELECT s FROM CaseFileShare s
        WHERE s.caseFile.id = :caseFileId
          AND s.revokedAt IS NULL
          AND s.expiresAt > :now
        ORDER BY s.createdAt DESC
        """)
    List<CaseFileShare> findActiveByCaseFileId(@Param("caseFileId") UUID caseFileId,
                                               @Param("now") Instant now);

    Optional<CaseFileShare> findByIdAndCaseFileId(UUID id, UUID caseFileId);
}
