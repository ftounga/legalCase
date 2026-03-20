package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UsageEventRepository extends JpaRepository<UsageEvent, UUID> {
    List<UsageEvent> findByCaseFileIdOrderByCreatedAtDesc(UUID caseFileId);
    List<UsageEvent> findByCaseFileIdIn(Collection<UUID> caseFileIds);

    @Query(value = """
            SELECT cf.workspace_id,
                   COALESCE(SUM(u.tokens_input), 0)    AS total_tokens_input,
                   COALESCE(SUM(u.tokens_output), 0)   AS total_tokens_output,
                   COALESCE(SUM(u.estimated_cost), 0)  AS total_cost
            FROM usage_events u
            JOIN case_files cf ON cf.id = u.case_file_id
            GROUP BY cf.workspace_id
            """, nativeQuery = true)
    List<Object[]> aggregateByWorkspaceId();
}
