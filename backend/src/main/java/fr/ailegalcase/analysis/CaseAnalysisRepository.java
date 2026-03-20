package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaseAnalysisRepository extends JpaRepository<CaseAnalysis, UUID> {

    boolean existsByCaseFileIdAndAnalysisStatusIn(UUID caseFileId, List<AnalysisStatus> statuses);

    Optional<CaseAnalysis> findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
            UUID caseFileId, AnalysisStatus analysisStatus);

    void deleteByCaseFileIdIn(Collection<UUID> caseFileIds);
}
