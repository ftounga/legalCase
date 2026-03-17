package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private fr.ailegalcase.auth.AuthAccountRepository authAccountRepository;

    private WorkspaceService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceService(workspaceRepository, workspaceMemberRepository, authAccountRepository);
    }

    // U-01 : premier login → workspace + membre OWNER créés
    @Test
    void createDefaultWorkspace_firstLogin_createsWorkspaceAndOwnerMember() {
        User user = new User();
        user.setEmail("john@example.com");

        when(workspaceMemberRepository.existsByUser(user)).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createDefaultWorkspace(user);

        ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(workspaceCaptor.capture());
        assertThat(workspaceCaptor.getValue().getName()).isEqualTo("john@example.com");
        assertThat(workspaceCaptor.getValue().getPlanCode()).isEqualTo("STARTER");
        assertThat(workspaceCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(workspaceCaptor.getValue().getSlug()).isNotBlank();

        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getMemberRole()).isEqualTo("OWNER");
        assertThat(memberCaptor.getValue().getUser()).isEqualTo(user);
    }

    // U-02 : login suivant (workspace existant) → aucune création
    @Test
    void createDefaultWorkspace_existingWorkspace_doesNotCreate() {
        User user = new User();
        user.setEmail("john@example.com");

        when(workspaceMemberRepository.existsByUser(user)).thenReturn(true);

        service.createDefaultWorkspace(user);

        verify(workspaceRepository, never()).save(any());
        verify(workspaceMemberRepository, never()).save(any());
    }
}
