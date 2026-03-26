package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnalysisDocumentRepository extends JpaRepository<AnalysisDocument, UUID> {

    List<AnalysisDocument> findByAnalysisIdOrderByCreatedAt(UUID analysisId);
}
