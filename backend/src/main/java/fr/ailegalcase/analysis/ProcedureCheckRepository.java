package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcedureCheckRepository extends JpaRepository<ProcedureCheck, UUID> {

    List<ProcedureCheck> findByCaseAnalysisIdOrderByOrdreAsc(UUID caseAnalysisId);

    List<ProcedureCheck> findByCaseAnalysisIdAndStatutOrderByOrdreAsc(UUID caseAnalysisId, ProcedureCheckStatus statut);

    Optional<ProcedureCheck> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    void deleteByCaseAnalysisId(UUID caseAnalysisId);
}
