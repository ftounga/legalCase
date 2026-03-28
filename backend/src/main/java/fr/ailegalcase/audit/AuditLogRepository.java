package fr.ailegalcase.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findTop50ByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    List<AuditLog> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    List<AuditLog> findByWorkspaceIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(UUID workspaceId, java.time.Instant from);

    List<AuditLog> findByWorkspaceIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(UUID workspaceId, java.time.Instant to);

    List<AuditLog> findByWorkspaceIdAndCreatedAtBetweenOrderByCreatedAtDesc(UUID workspaceId, java.time.Instant from, java.time.Instant to);
}
