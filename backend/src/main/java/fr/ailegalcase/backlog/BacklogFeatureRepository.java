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
public interface BacklogFeatureRepository extends JpaRepository<BacklogFeatureEntity, UUID> {

    Optional<BacklogFeatureEntity> findByCode(String code);

    @Query("""
            SELECT f FROM BacklogFeatureEntity f
            WHERE (:status IS NULL OR f.status = :status)
              AND (:domain IS NULL OR f.domain = :domain)
              AND (:priority IS NULL OR f.priority = :priority)
              AND (:search IS NULL OR LOWER(f.code) LIKE LOWER(CONCAT('%', :search, '%'))
                                    OR LOWER(f.title) LIKE LOWER(CONCAT('%', :search, '%'))
                                    OR LOWER(COALESCE(f.description, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND f.orphaned = false
            """)
    Page<BacklogFeatureEntity> search(@Param("status") BacklogStatus status,
                                      @Param("domain") BacklogDomain domain,
                                      @Param("priority") BacklogPriority priority,
                                      @Param("search") String search,
                                      Pageable pageable);

    List<BacklogFeatureEntity> findByOrphanedFalse();
}
