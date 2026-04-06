package fr.ailegalcase.notification;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InAppNotificationServiceTest {

    @Mock InAppNotificationRepository notificationRepository;
    @Mock WorkspaceMemberRepository workspaceMemberRepository;
    @Mock CurrentUserResolver currentUserResolver;

    @InjectMocks InAppNotificationService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();

    @Test
    void create_persiste_notification() {
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InAppNotification result = service.create(userId, workspaceId,
                NotificationType.ANALYSIS_DONE, "Analyse terminée",
                "Votre dossier a été analysé", "/case-files/123");

        ArgumentCaptor<InAppNotification> captor = ArgumentCaptor.forClass(InAppNotification.class);
        verify(notificationRepository).save(captor.capture());

        InAppNotification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(saved.getType()).isEqualTo(NotificationType.ANALYSIS_DONE);
        assertThat(saved.getTitle()).isEqualTo("Analyse terminée");
        assertThat(saved.getMessage()).isEqualTo("Votre dossier a été analysé");
        assertThat(saved.getLink()).isEqualTo("/case-files/123");
    }

    @Test
    void markAsRead_met_is_read_true() {
        InAppNotification notif = new InAppNotification();
        notif.setId(UUID.randomUUID());
        notif.setUserId(userId);
        notif.setWorkspaceId(workspaceId);
        notif.setRead(false);

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        Workspace workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(workspaceId);

        WorkspaceMember member = mock(WorkspaceMember.class);
        when(member.getUser()).thenReturn(user);
        when(member.getWorkspace()).thenReturn(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(notificationRepository.findByIdAndWorkspaceId(notif.getId(), workspaceId))
                .thenReturn(Optional.of(notif));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markAsRead(null, null, notif.getId());

        assertThat(notif.isRead()).isTrue();
        assertThat(notif.getReadAt()).isNotNull();
        verify(notificationRepository).save(notif);
    }

    @Test
    void markAsRead_403_si_autre_utilisateur() {
        UUID otherUserId = UUID.randomUUID();

        InAppNotification notif = new InAppNotification();
        notif.setId(UUID.randomUUID());
        notif.setUserId(otherUserId);
        notif.setWorkspaceId(workspaceId);

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        Workspace workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(workspaceId);

        WorkspaceMember member = mock(WorkspaceMember.class);
        when(member.getUser()).thenReturn(user);
        when(member.getWorkspace()).thenReturn(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(notificationRepository.findByIdAndWorkspaceId(notif.getId(), workspaceId))
                .thenReturn(Optional.of(notif));

        assertThatThrownBy(() -> service.markAsRead(null, null, notif.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void getUnreadCount_retourne_le_bon_nombre() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        Workspace workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(workspaceId);

        WorkspaceMember member = mock(WorkspaceMember.class);
        when(member.getUser()).thenReturn(user);
        when(member.getWorkspace()).thenReturn(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(notificationRepository.countByUserIdAndWorkspaceIdAndIsReadFalse(userId, workspaceId))
                .thenReturn(5L);

        UnreadCountResponse result = service.getUnreadCount(null, null);
        assertThat(result.count()).isEqualTo(5);
    }
}
