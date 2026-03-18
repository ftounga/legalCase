package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(WorkspaceService.class)
class WorkspaceServiceIT {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private User savedUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setStatus("ACTIVE");
        return userRepository.save(user);
    }

    // I-01 : premier login → workspace + membre persistés en base
    @Test
    void createDefaultWorkspace_firstLogin_persistsWorkspaceAndMember() {
        User user = savedUser("john@example.com");

        workspaceService.createDefaultWorkspace(user);

        List<Workspace> workspaces = workspaceRepository.findAll();
        assertThat(workspaces).hasSize(1);
        assertThat(workspaces.get(0).getName()).isEqualTo("john@example.com");
        assertThat(workspaces.get(0).getPlanCode()).isEqualTo("STARTER");
        assertThat(workspaces.get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(workspaces.get(0).getCreatedAt()).isNotNull();

        List<WorkspaceMember> members = workspaceMemberRepository.findAll();
        assertThat(members).hasSize(1);
        assertThat(members.get(0).getMemberRole()).isEqualTo("OWNER");
        assertThat(members.get(0).getCreatedAt()).isNotNull();
    }

    // I-02 : owner_user_id = id du user créé
    @Test
    void createDefaultWorkspace_ownerIsCorrectlySet() {
        User user = savedUser("jane@example.com");

        workspaceService.createDefaultWorkspace(user);

        Workspace workspace = workspaceRepository.findAll().get(0);
        assertThat(workspace.getOwner().getId()).isEqualTo(user.getId());
    }

    // I-03 : login suivant → idempotent, aucun doublon
    @Test
    void createDefaultWorkspace_secondLogin_isIdempotent() {
        User user = savedUser("idempotent@example.com");

        workspaceService.createDefaultWorkspace(user);
        workspaceService.createDefaultWorkspace(user);

        assertThat(workspaceRepository.findAll()).hasSize(1);
        assertThat(workspaceMemberRepository.findAll()).hasSize(1);
        assertThat(subscriptionRepository.findAll()).hasSize(1);
    }

    // I-04 : subscription STARTER ACTIVE créée avec le workspace
    @Test
    void createDefaultWorkspace_createsStarterSubscription() {
        User user = savedUser("sub@example.com");

        workspaceService.createDefaultWorkspace(user);

        Workspace workspace = workspaceRepository.findAll().get(0);
        List<Subscription> subs = subscriptionRepository.findAll();
        assertThat(subs).hasSize(1);
        assertThat(subs.get(0).getWorkspaceId()).isEqualTo(workspace.getId());
        assertThat(subs.get(0).getPlanCode()).isEqualTo("STARTER");
        assertThat(subs.get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(subs.get(0).getStartedAt()).isNotNull();
        assertThat(subs.get(0).getExpiresAt()).isNull();
    }
}
