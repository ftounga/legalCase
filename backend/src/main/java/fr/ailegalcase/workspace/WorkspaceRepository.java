package fr.ailegalcase.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    long countByPlanCode(String planCode);

    long countByCreatedAtAfter(Instant since);
}
