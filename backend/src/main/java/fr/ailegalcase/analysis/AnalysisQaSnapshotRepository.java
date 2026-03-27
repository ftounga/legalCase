package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnalysisQaSnapshotRepository extends JpaRepository<AnalysisQaSnapshot, UUID> {

    List<AnalysisQaSnapshot> findByAnalysisIdOrderByOrderIndex(UUID analysisId);
}
