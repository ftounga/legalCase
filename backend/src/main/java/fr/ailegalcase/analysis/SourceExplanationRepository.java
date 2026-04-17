package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SourceExplanationRepository extends JpaRepository<SourceExplanation, UUID> {

    List<SourceExplanation> findByCaseAnalysisId(UUID caseAnalysisId);

    void deleteByCaseAnalysisId(UUID caseAnalysisId);
}
