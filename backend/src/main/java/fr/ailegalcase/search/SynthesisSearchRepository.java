package fr.ailegalcase.search;

import fr.ailegalcase.analysis.CaseAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SynthesisSearchRepository extends JpaRepository<CaseAnalysis, UUID> {

    @Query(value = """
            SELECT ca.*
            FROM case_analyses ca
            JOIN case_files cf ON cf.id = ca.case_file_id
            WHERE cf.workspace_id = :workspaceId
              AND ca.analysis_status = 'DONE'
              AND LOWER(ca.analysis_result) LIKE LOWER(CONCAT('%', :query, '%'))
              AND ca.version = (
                  SELECT MAX(ca2.version)
                  FROM case_analyses ca2
                  WHERE ca2.case_file_id = ca.case_file_id
                    AND ca2.analysis_status = 'DONE'
              )
            ORDER BY cf.created_at DESC
            LIMIT 50
            """, nativeQuery = true)
    List<CaseAnalysis> searchByWorkspace(@Param("workspaceId") UUID workspaceId,
                                         @Param("query") String query);
}
