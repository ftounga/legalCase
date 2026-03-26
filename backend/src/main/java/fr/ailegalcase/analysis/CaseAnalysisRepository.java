package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaseAnalysisRepository extends JpaRepository<CaseAnalysis, UUID> {

    boolean existsByCaseFileIdAndAnalysisStatusIn(UUID caseFileId, List<AnalysisStatus> statuses);

    Optional<CaseAnalysis> findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
            UUID caseFileId, AnalysisStatus analysisStatus);

    List<CaseAnalysis> findByCaseFileIdAndAnalysisStatusOrderByVersionDesc(
            UUID caseFileId, AnalysisStatus analysisStatus);

    Optional<CaseAnalysis> findByCaseFileIdAndAnalysisStatusAndVersion(
            UUID caseFileId, AnalysisStatus analysisStatus, int version);

    Optional<CaseAnalysis> findFirstByCaseFileIdAndAnalysisTypeAndVersionLessThanOrderByVersionDesc(
            UUID caseFileId, AnalysisType analysisType, int version);

    @Query("SELECT COALESCE(MAX(ca.version), 0) FROM CaseAnalysis ca WHERE ca.caseFile.id = :caseFileId")
    int findMaxVersionByCaseFileId(@Param("caseFileId") UUID caseFileId);

    long countByCaseFileIdAndAnalysisStatus(UUID caseFileId, AnalysisStatus analysisStatus);

    void deleteByCaseFileIdIn(Collection<UUID> caseFileIds);
}
