package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {
    boolean existsByUser(User user);
}
