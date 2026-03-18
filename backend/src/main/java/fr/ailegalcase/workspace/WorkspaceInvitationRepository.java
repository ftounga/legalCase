package fr.ailegalcase.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, UUID> {
    Optional<WorkspaceInvitation> findByToken(String token);
    boolean existsByWorkspaceIdAndEmailAndStatus(UUID workspaceId, String email, String status);
}
