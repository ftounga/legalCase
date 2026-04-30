package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StrategicOptionRepository extends JpaRepository<StrategicOption, UUID> {

    List<StrategicOption> findByCaseAnalysisIdOrderByOrdreAsc(UUID caseAnalysisId);

    List<StrategicOption> findByCaseAnalysisIdAndStatutOrderByOrdreAsc(UUID caseAnalysisId, StrategicOptionStatus statut);

    @Query("SELECT s FROM StrategicOption s WHERE s.id = :id AND s.workspace.id = :workspaceId")
    Optional<StrategicOption> findByIdAndWorkspaceId(@Param("id") UUID id, @Param("workspaceId") UUID workspaceId);

    @Modifying
    @Query("DELETE FROM StrategicOption s WHERE s.caseAnalysis.id = :caseAnalysisId")
    void deleteByCaseAnalysisId(@Param("caseAnalysisId") UUID caseAnalysisId);
}
