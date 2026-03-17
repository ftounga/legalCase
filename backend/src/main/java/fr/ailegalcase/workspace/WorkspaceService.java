package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository workspaceMemberRepository) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public void createDefaultWorkspace(User user) {
        if (workspaceMemberRepository.existsByUser(user)) {
            return;
        }

        Workspace workspace = new Workspace();
        workspace.setName(user.getEmail());
        workspace.setSlug(UUID.randomUUID().toString());
        workspace.setOwner(user);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        workspaceMemberRepository.save(member);
    }
}
