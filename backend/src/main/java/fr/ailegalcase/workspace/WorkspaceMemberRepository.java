package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {
    boolean existsByUser(User user);
    Optional<WorkspaceMember> findByUserAndPrimaryTrue(User user);
    List<WorkspaceMember> findByWorkspace_Id(UUID workspaceId);
    Optional<WorkspaceMember> findByWorkspace_IdAndUser_Id(UUID workspaceId, UUID userId);
    List<WorkspaceMember> findByUserAndPrimaryFalse(User user);
    List<WorkspaceMember> findByUser(User user);
}
