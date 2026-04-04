package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcedureCheckRepository extends JpaRepository<ProcedureCheck, UUID> {

    List<ProcedureCheck> findByCaseAnalysisIdOrderByOrdreAsc(UUID caseAnalysisId);

    List<ProcedureCheck> findByCaseAnalysisIdAndStatutOrderByOrdreAsc(UUID caseAnalysisId, ProcedureCheckStatus statut);

    Optional<ProcedureCheck> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    void deleteByCaseAnalysisId(UUID caseAnalysisId);

    @Query("SELECT pc FROM ProcedureCheck pc WHERE pc.workspace.id = :workspaceId AND pc.statut = fr.ailegalcase.analysis.ProcedureCheckStatus.NON_COMPLIANT AND pc.updatedAt < :cutoff")
    List<ProcedureCheck> findStaleNonCompliantByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("cutoff") Instant cutoff);
}
