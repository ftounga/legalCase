package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DocumentAnalysisRepository extends JpaRepository<DocumentAnalysis, UUID> {

    List<DocumentAnalysis> findByExtractionId(UUID extractionId);

    boolean existsByExtractionIdAndAnalysisStatusIn(UUID extractionId, List<AnalysisStatus> statuses);

    long countByDocumentCaseFileIdAndAnalysisStatus(UUID caseFileId, AnalysisStatus status);

    List<DocumentAnalysis> findByDocumentCaseFileIdAndAnalysisStatus(UUID caseFileId, AnalysisStatus status);

    void deleteByExtractionIdIn(Collection<UUID> extractionIds);
}
