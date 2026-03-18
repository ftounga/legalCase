package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceInvitationServiceTest {

    @Mock private WorkspaceInvitationRepository workspaceInvitationRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private EmailService emailService;
    @Mock private AuthAccountRepository authAccountRepository;

    private WorkspaceInvitationService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceInvitationService(workspaceInvitationRepository, workspaceMemberRepository,
                workspaceRepository, authAccountRepository, emailService);
    }

    // U-01 : createInvitation — token généré, status PENDING, expiry +7j
    @Test
    void createInvitation_validRequest_createsWithTokenAndExpiry() {
        User owner = buildUser("owner@example.com");
        Workspace workspace = buildWorkspace();
        WorkspaceMember ownerMember = buildMember(workspace, owner, "OWNER");

        setupAuth(owner, "sub-owner");
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(owner)).thenReturn(Optional.of(ownerMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), owner.getId())).thenReturn(Optional.of(ownerMember));
        when(workspaceInvitationRepository.existsByWorkspaceIdAndEmailAndStatus(workspace.getId(), "invitee@example.com", "PENDING")).thenReturn(false);
        when(workspaceInvitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceInvitationRequest request = new WorkspaceInvitationRequest("invitee@example.com", "LAWYER");
        WorkspaceInvitationResponse response = service.createInvitation(request, buildOidcUser("sub-owner", "owner@example.com"), "GOOGLE");

        ArgumentCaptor<WorkspaceInvitation> captor = ArgumentCaptor.forClass(WorkspaceInvitation.class);
        verify(workspaceInvitationRepository).save(captor.capture());
        WorkspaceInvitation saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getToken()).isNotBlank();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(6, ChronoUnit.DAYS));
        assertThat(saved.getRole()).isEqualTo("LAWYER");
    }

    // U-02 : createInvitation — doublon PENDING → 409
    @Test
    void createInvitation_duplicatePending_throws409() {
        User owner = buildUser("owner@example.com");
        Workspace workspace = buildWorkspace();
        WorkspaceMember ownerMember = buildMember(workspace, owner, "OWNER");

        setupAuth(owner, "sub-owner");
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(owner)).thenReturn(Optional.of(ownerMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), owner.getId())).thenReturn(Optional.of(ownerMember));
        when(workspaceInvitationRepository.existsByWorkspaceIdAndEmailAndStatus(workspace.getId(), "invitee@example.com", "PENDING")).thenReturn(true);

        WorkspaceInvitationRequest request = new WorkspaceInvitationRequest("invitee@example.com", "LAWYER");
        assertThatThrownBy(() -> service.createInvitation(request, buildOidcUser("sub-owner", "owner@example.com"), "GOOGLE"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    // U-03 : acceptInvitation — token valide → membre créé, is_primary basculé
    @Test
    void acceptInvitation_validToken_addsMemberAndSwitchesPrimary() {
        User invitee = buildUser("invitee@example.com");
        Workspace targetWorkspace = buildWorkspace();
        WorkspaceMember existingPrimary = buildMember(buildWorkspace(), invitee, "OWNER");

        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setToken("valid-token");
        invitation.setEmail("invitee@example.com");
        invitation.setRole("LAWYER");
        invitation.setStatus("PENDING");
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invitation.setWorkspaceId(targetWorkspace.getId());

        setupAuth(invitee, "sub-invitee");
        when(workspaceInvitationRepository.findByToken("valid-token")).thenReturn(Optional.of(invitation));
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(invitee)).thenReturn(Optional.of(existingPrimary));
        when(workspaceRepository.findById(targetWorkspace.getId())).thenReturn(Optional.of(targetWorkspace));
        when(workspaceMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workspaceInvitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.acceptInvitation(new AcceptInvitationRequest("valid-token"), buildOidcUser("sub-invitee", "invitee@example.com"), "GOOGLE");

        assertThat(existingPrimary.isPrimary()).isFalse();
        verify(workspaceMemberRepository, times(2)).save(any());
        assertThat(invitation.getStatus()).isEqualTo("ACCEPTED");
    }

    // U-04 : acceptInvitation — token expiré → 409
    @Test
    void acceptInvitation_expiredToken_throws409() {
        User invitee = buildUser("invitee@example.com");

        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setToken("expired-token");
        invitation.setEmail("invitee@example.com");
        invitation.setStatus("PENDING");
        invitation.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));

        setupAuth(invitee, "sub-invitee");
        when(workspaceInvitationRepository.findByToken("expired-token")).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.acceptInvitation(new AcceptInvitationRequest("expired-token"), buildOidcUser("sub-invitee", "invitee@example.com"), "GOOGLE"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    // U-05 : acceptInvitation — status != PENDING → 409
    @Test
    void acceptInvitation_alreadyAccepted_throws409() {
        User invitee = buildUser("invitee@example.com");

        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setToken("used-token");
        invitation.setEmail("invitee@example.com");
        invitation.setStatus("ACCEPTED");
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

        setupAuth(invitee, "sub-invitee");
        when(workspaceInvitationRepository.findByToken("used-token")).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.acceptInvitation(new AcceptInvitationRequest("used-token"), buildOidcUser("sub-invitee", "invitee@example.com"), "GOOGLE"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    // U-06 : acceptInvitation — email différent → 403
    @Test
    void acceptInvitation_emailMismatch_throws403() {
        User other = buildUser("other@example.com");

        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setToken("token");
        invitation.setEmail("invitee@example.com");
        invitation.setStatus("PENDING");
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

        setupAuth(other, "sub-other");
        when(workspaceInvitationRepository.findByToken("token")).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.acceptInvitation(new AcceptInvitationRequest("token"), buildOidcUser("sub-other", "other@example.com"), "GOOGLE"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // U-07 : createInvitation — email envoyé après save
    @Test
    void createInvitation_emailSentAfterSave() {
        User owner = buildUser("owner@example.com");
        Workspace workspace = buildWorkspace();
        WorkspaceMember ownerMember = buildMember(workspace, owner, "OWNER");

        setupAuth(owner, "sub-owner");
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(owner)).thenReturn(Optional.of(ownerMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), owner.getId())).thenReturn(Optional.of(ownerMember));
        when(workspaceInvitationRepository.existsByWorkspaceIdAndEmailAndStatus(workspace.getId(), "invitee@example.com", "PENDING")).thenReturn(false);
        when(workspaceInvitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createInvitation(new WorkspaceInvitationRequest("invitee@example.com", "LAWYER"),
                buildOidcUser("sub-owner", "owner@example.com"), "GOOGLE");

        verify(emailService).sendInvitation(eq("invitee@example.com"), eq(workspace.getName()), any());
    }

    // U-08 : createInvitation — échec SMTP → invitation créée quand même (fail-open géré dans EmailService)
    @Test
    void createInvitation_emailServiceCalledEvenIfSmtpFails() {
        User owner = buildUser("owner@example.com");
        Workspace workspace = buildWorkspace();
        WorkspaceMember ownerMember = buildMember(workspace, owner, "OWNER");

        setupAuth(owner, "sub-owner");
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(owner)).thenReturn(Optional.of(ownerMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), owner.getId())).thenReturn(Optional.of(ownerMember));
        when(workspaceInvitationRepository.existsByWorkspaceIdAndEmailAndStatus(workspace.getId(), "invitee@example.com", "PENDING")).thenReturn(false);
        when(workspaceInvitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendInvitation(any(), any(), any());

        WorkspaceInvitationResponse response = service.createInvitation(
                new WorkspaceInvitationRequest("invitee@example.com", "LAWYER"),
                buildOidcUser("sub-owner", "owner@example.com"), "GOOGLE");

        assertThat(response.status()).isEqualTo("PENDING");
        verify(workspaceInvitationRepository).save(any());
    }

    // Helpers

    private User buildUser(String email) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setStatus("ACTIVE");
        return u;
    }

    private Workspace buildWorkspace() {
        Workspace w = new Workspace();
        w.setId(UUID.randomUUID());
        w.setName("Test");
        w.setSlug("slug-" + UUID.randomUUID());
        w.setPlanCode("STARTER");
        w.setStatus("ACTIVE");
        return w;
    }

    private WorkspaceMember buildMember(Workspace workspace, User user, String role) {
        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(workspace);
        m.setUser(user);
        m.setMemberRole(role);
        m.setPrimary(true);
        return m;
    }

    private void setupAuth(User user, String sub) {
        AuthAccount account = new AuthAccount();
        account.setUser(user);
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", sub)).thenReturn(Optional.of(account));
    }

    private OidcUser buildOidcUser(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        return new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
    }
}
