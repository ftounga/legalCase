package fr.ailegalcase.backlog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BacklogMarketingTaskRepository extends JpaRepository<BacklogMarketingTaskEntity, UUID> {

    Optional<BacklogMarketingTaskEntity> findByCode(String code);

    @Query("""
            SELECT t FROM BacklogMarketingTaskEntity t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:searchPattern IS NULL OR LOWER(t.code) LIKE :searchPattern
                                          OR LOWER(t.title) LIKE :searchPattern
                                          OR (t.description IS NOT NULL AND LOWER(t.description) LIKE :searchPattern))
              AND t.orphaned = false
            """)
    Page<BacklogMarketingTaskEntity> search(@Param("status") BacklogMarketingStatus status,
                                            @Param("searchPattern") String searchPattern,
                                            Pageable pageable);

    List<BacklogMarketingTaskEntity> findByOrphanedFalse();
}
