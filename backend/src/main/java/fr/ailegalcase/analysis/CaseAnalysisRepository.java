package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CaseAnalysisRepository extends JpaRepository<CaseAnalysis, UUID> {

    boolean existsByCaseFileIdAndAnalysisStatusIn(UUID caseFileId, List<AnalysisStatus> statuses);
}
