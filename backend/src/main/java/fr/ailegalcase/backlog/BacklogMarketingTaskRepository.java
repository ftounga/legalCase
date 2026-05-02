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
              AND (:search IS NULL OR LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%'))
                                    OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))
                                    OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND t.orphaned = false
            """)
    Page<BacklogMarketingTaskEntity> search(@Param("status") BacklogMarketingStatus status,
                                            @Param("search") String search,
                                            Pageable pageable);

    List<BacklogMarketingTaskEntity> findByOrphanedFalse();
}
