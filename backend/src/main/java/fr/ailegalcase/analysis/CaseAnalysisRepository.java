package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CaseAnalysisRepository extends JpaRepository<CaseAnalysis, UUID> {

    boolean existsByCaseFileIdAndAnalysisStatusIn(UUID caseFileId, List<AnalysisStatus> statuses);

    Optional<CaseAnalysis> findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
            UUID caseFileId, AnalysisStatus analysisStatus);

    /**
     * F-185 SF-185-01 — récupère l'analyse en cours (PROCESSING ou PARTIAL) la plus
     * récente d'un dossier, pour servir l'endpoint partial pendant le streaming.
     */
    Optional<CaseAnalysis> findFirstByCaseFileIdAndAnalysisStatusInOrderByVersionDesc(
            UUID caseFileId, Collection<AnalysisStatus> statuses);

    Optional<CaseAnalysis> findFirstByCaseFileIdAndAnalysisTypeAndAnalysisStatusOrderByUpdatedAtDesc(
            UUID caseFileId, AnalysisType analysisType, AnalysisStatus analysisStatus);

    List<CaseAnalysis> findByCaseFileIdAndAnalysisStatusOrderByVersionDesc(
            UUID caseFileId, AnalysisStatus analysisStatus);

    Optional<CaseAnalysis> findByCaseFileIdAndAnalysisStatusAndVersion(
            UUID caseFileId, AnalysisStatus analysisStatus, int version);

    Optional<CaseAnalysis> findFirstByCaseFileIdAndAnalysisTypeAndVersionLessThanOrderByVersionDesc(
            UUID caseFileId, AnalysisType analysisType, int version);

    @Query("SELECT COALESCE(MAX(ca.version), 0) FROM CaseAnalysis ca WHERE ca.caseFile.id = :caseFileId")
    int findMaxVersionByCaseFileId(@Param("caseFileId") UUID caseFileId);

    long countByCaseFileIdAndAnalysisStatus(UUID caseFileId, AnalysisStatus analysisStatus);

    Optional<CaseAnalysis> findByIdAndCaseFileId(UUID id, UUID caseFileId);

    void deleteByCaseFileIdIn(Collection<UUID> caseFileIds);

    @Query("SELECT COUNT(ca) FROM CaseAnalysis ca WHERE ca.analysisStatus = fr.ailegalcase.analysis.AnalysisStatus.DONE AND ca.createdAt >= :since")
    long countDoneCreatedAfter(@Param("since") Instant since);

    @Query("SELECT DISTINCT ca.caseFile.workspace.id FROM CaseAnalysis ca WHERE ca.analysisStatus = fr.ailegalcase.analysis.AnalysisStatus.DONE AND ca.createdAt >= :since")
    Set<UUID> findDistinctWorkspaceIdsWithDoneAnalysisSince(@Param("since") Instant since);

    @Query("SELECT COUNT(ca) FROM CaseAnalysis ca WHERE ca.caseFile.workspace.id = :workspaceId AND ca.analysisStatus = fr.ailegalcase.analysis.AnalysisStatus.DONE AND ca.createdAt >= :since")
    long countDoneByWorkspaceIdAndCreatedAtAfter(@Param("workspaceId") UUID workspaceId, @Param("since") Instant since);

    List<CaseAnalysis> findTop5ByCaseFile_WorkspaceAndAnalysisStatusOrderByCreatedAtDesc(
            fr.ailegalcase.workspace.Workspace workspace, AnalysisStatus analysisStatus);

    List<CaseAnalysis> findByCaseFile_WorkspaceAndAnalysisStatusAndCreatedAtAfter(
            fr.ailegalcase.workspace.Workspace workspace, AnalysisStatus analysisStatus, Instant createdAt);
}
