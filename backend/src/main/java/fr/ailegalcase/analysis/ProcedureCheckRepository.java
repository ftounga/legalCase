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

    /**
     * SF-125-01 fix : détecte une action avocat sur la checklist procédurale depuis un instant donné.
     * Un check avec statut != TO_CHECK dont {@code updatedAt > cutoff} indique que l'avocat
     * a coché un point (VERIFIED / NON_COMPLIANT) après ce moment.
     *
     * Le cutoff doit inclure une marge temporelle (ex. +10s) par rapport à l'updatedAt
     * de la dernière analyse enrichie, pour exclure les propagations automatiques
     * (createChecksWithVerifiedPropagation) qui font des saves sur des checks propagés
     * dans les millisecondes suivant le save de l'analyse.
     */
    @Query("""
            SELECT (COUNT(pc) > 0) FROM ProcedureCheck pc
            WHERE pc.caseAnalysis.caseFile.id = :caseFileId
              AND pc.statut <> fr.ailegalcase.analysis.ProcedureCheckStatus.TO_CHECK
              AND pc.updatedAt > :cutoff
            """)
    boolean existsValidatedCheckUpdatedAfter(@Param("caseFileId") UUID caseFileId,
                                              @Param("cutoff") Instant cutoff);
}
