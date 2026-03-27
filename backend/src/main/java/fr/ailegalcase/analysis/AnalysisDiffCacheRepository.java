package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisDiffCacheRepository extends JpaRepository<AnalysisDiffCache, UUID> {

    Optional<AnalysisDiffCache> findByFromIdAndToId(UUID fromId, UUID toId);
}
