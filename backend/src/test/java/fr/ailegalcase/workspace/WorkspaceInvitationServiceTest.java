package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.billing.StripeSeatService;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.PaymentRequiredCode;
import fr.ailegalcase.shared.PaymentRequiredException;
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
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private PlanLimitService planLimitService;
    @Mock private StripeSeatService stripeSeatService;

    // SF-156-01 : guard ACTIVE-only — vraie instance (composant simple).
    private final WorkspaceAccessGuard workspaceAccessGuard = new WorkspaceAccessGuard();

    private WorkspaceInvitationService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceInvitationService(workspaceInvitationRepository, workspaceMemberRepository,
                workspaceRepository, currentUserResolver, emailService, planLimitService, stripeSeatService,
                workspaceAccessGuard);
        // SF-123-02 : par défaut les tests existants sont sur un plan sans gate (TEAM/PRO avec large cap).
        lenient().when(planLimitService.getMaxSeatsForWorkspace(any())).thenReturn(100);
    }

    // U-01 : createInvitation — token généré, status PENDING, expiry +7j
    @Test
    void createInvitation_validRequest_createsWithTokenAndExpiry() {
        User owner = buildUser("owner@example.com");
        Workspace workspace = buildWorkspace();
        WorkspaceMember ownerMember = buildMember(workspace, owner, "OWNER");

        setupAuth(owner);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(owner)).thenReturn(Optional.of(ownerMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), owner.getId())).thenReturn(Optional.of(ownerMember));
        when(workspaceInvitationRepository.existsByWorkspaceIdAndEmailAndStatus(workspace.getId(), "invitee@example.com", "PENDING")).thenReturn(false);
        when(workspaceInvitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceInvitationRequest request = new WorkspaceInvitationRequest("invitee@example.com", "LAWYER");
        WorkspaceInvitationResponse response = service.createInvitation(request, buildOidcUser("sub-owner", "owner@example.com"), "GOOGLE", null);

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

        setupAuth(owner);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(owner)).thenReturn(Optional.of(ownerMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), owner.getId())).thenReturn(Optional.of(ownerMember));
        when(workspaceInvitationRepository.existsByWorkspaceIdAndEmailAndStatus(workspace.getId(), "invitee@example.com", "PENDING")).thenReturn(true);

        WorkspaceInvitationRequest request = new WorkspaceInvitationRequest("invitee@example.com", "LAWYER");
        assertThatThrownBy(() -> service.createInvitation(request, buildOidcUser("sub-owner", "owner@example.com"), "GOOGLE", null))
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

        setupAuth(invitee);
        when(workspaceInvitationRepository.findByToken("valid-token")).thenReturn(Optional.of(invitation));
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(invitee)).thenReturn(Optional.of(existingPrimary));
        when(workspaceRepository.findById(targetWorkspace.getId())).thenReturn(Optional.of(targetWorkspace));
        when(workspaceMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workspaceInvitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.acceptInvitation(new AcceptInvitationRequest("valid-token"), buildOidcUser("sub-invitee", "invitee@example.com"), "GOOGLE", null);

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

        setupAuth(invitee);
        when(workspaceInvitationRepository.findByToken("expired-token")).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.acceptInvitation(new AcceptInvitationRequest("expired-token"), buildOidcUser("sub-invitee", "invitee@example.com"), "GOOGLE", null))
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

        setupAuth(invitee);
        when(workspaceInvitationRepository.findByToken("used-token")).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.acceptInvitation(new AcceptInvitationRequest("used-token"), buildOidcUser("sub-invitee", "invitee@example.com"), "GOOGLE", null))
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

        setupAuth(other);
        when(workspaceInvitationRepository.findByToken("token")).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.acceptInvitation(new AcceptInvitationRequest("token"), buildOidcUser("sub-other", "other@example.com"), "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // U-07 : createInvitation — email envoyé après save
    @Test
    void createInvitation_emailSentAfterSave() {
        User owner = buildUser("owner@example.com");
        Workspace workspace = buildWorkspace();
        WorkspaceMember ownerMember = buildMember(workspace, owner, "OWNER");

        setupAuth(owner);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(owner)).thenReturn(Optional.of(ownerMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), owner.getId())).thenReturn(Optional.of(ownerMember));
        when(workspaceInvitationRepository.existsByWorkspaceIdAndEmailAndStatus(workspace.getId(), "invitee@example.com", "PENDING")).thenReturn(false);
        when(workspaceInvitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createInvitation(new WorkspaceInvitationRequest("invitee@example.com", "LAWYER"),
                buildOidcUser("sub-owner", "owner@example.com"), "GOOGLE", null);

        verify(emailService).sendInvitation(eq("invitee@example.com"), eq(workspace.getName()), any());
    }

    // U-08 : createInvitation — échec SMTP → invitation créée quand même (fail-open géré dans EmailService)
    @Test
    void createInvitation_emailServiceCalledEvenIfSmtpFails() {
        User owner = buildUser("owner@example.com");
        Workspace workspace = buildWorkspace();
        WorkspaceMember ownerMember = buildMember(workspace, owner, "OWNER");

        setupAuth(owner);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(owner)).thenReturn(Optional.of(ownerMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), owner.getId())).thenReturn(Optional.of(ownerMember));
        when(workspaceInvitationRepository.existsByWorkspaceIdAndEmailAndStatus(workspace.getId(), "invitee@example.com", "PENDING")).thenReturn(false);
        when(workspaceInvitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendInvitation(any(), any(), any());

        WorkspaceInvitationResponse response = service.createInvitation(
                new WorkspaceInvitationRequest("invitee@example.com", "LAWYER"),
                buildOidcUser("sub-owner", "owner@example.com"), "GOOGLE", null);

        assertThat(response.status()).isEqualTo("PENDING");
        verify(workspaceInvitationRepository).save(any());
    }

    // U-SF123-02-01 : SOLO + 2e invitation → 402 PAYMENT_REQUIRED
    @Test
    void createInvitation_soloPlanSecondMember_throws402() {
        User owner = buildUser("owner@example.com");
        Workspace workspace = buildWorkspace();
        WorkspaceMember ownerMember = buildMember(workspace, owner, "OWNER");

        setupAuth(owner);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(owner)).thenReturn(Optional.of(ownerMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), owner.getId())).thenReturn(Optional.of(ownerMember));
        when(workspaceInvitationRepository.existsByWorkspaceIdAndEmailAndStatus(workspace.getId(), "invitee@example.com", "PENDING")).thenReturn(false);
        when(workspaceMemberRepository.findByWorkspace_Id(workspace.getId())).thenReturn(List.of(ownerMember));
        when(workspaceInvitationRepository.findByWorkspaceIdAndStatus(workspace.getId(), "PENDING")).thenReturn(List.of());
        when(planLimitService.getMaxSeatsForWorkspace(workspace.getId())).thenReturn(1);
        when(planLimitService.getPlanCodeForWorkspace(workspace.getId())).thenReturn("SOLO");

        WorkspaceInvitationRequest request = new WorkspaceInvitationRequest("invitee@example.com", "LAWYER");
        assertThatThrownBy(() -> service.createInvitation(request, buildOidcUser("sub-owner", "owner@example.com"), "GOOGLE", null))
                .isInstanceOf(PaymentRequiredException.class)
                .satisfies(ex -> assertThat(((PaymentRequiredException) ex).getCode())
                        .isEqualTo(PaymentRequiredCode.SEAT_LIMIT_EXCEEDED))
                .hasMessageContaining("TEAM");
    }

    // U-SF123-02-02 : TEAM avec 6 seats utilisés (3 membres + 3 invites pending) → 402
    @Test
    void createInvitation_teamPlanAtCap_throws402() {
        User owner = buildUser("owner@example.com");
        Workspace workspace = buildWorkspace();
        WorkspaceMember ownerMember = buildMember(workspace, owner, "OWNER");
        WorkspaceMember m2 = buildMember(workspace, buildUser("m2@ex.com"), "LAWYER");
        WorkspaceMember m3 = buildMember(workspace, buildUser("m3@ex.com"), "LAWYER");

        setupAuth(owner);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(owner)).thenReturn(Optional.of(ownerMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), owner.getId())).thenReturn(Optional.of(ownerMember));
        when(workspaceInvitationRepository.existsByWorkspaceIdAndEmailAndStatus(workspace.getId(), "invitee@example.com", "PENDING")).thenReturn(false);
        when(workspaceMemberRepository.findByWorkspace_Id(workspace.getId())).thenReturn(List.of(ownerMember, m2, m3));
        when(workspaceInvitationRepository.findByWorkspaceIdAndStatus(workspace.getId(), "PENDING"))
                .thenReturn(List.of(new WorkspaceInvitation(), new WorkspaceInvitation(), new WorkspaceInvitation()));
        when(planLimitService.getMaxSeatsForWorkspace(workspace.getId())).thenReturn(6);
        when(planLimitService.getPlanCodeForWorkspace(workspace.getId())).thenReturn("TEAM");

        WorkspaceInvitationRequest request = new WorkspaceInvitationRequest("invitee@example.com", "LAWYER");
        assertThatThrownBy(() -> service.createInvitation(request, buildOidcUser("sub-owner", "owner@example.com"), "GOOGLE", null))
                .isInstanceOf(PaymentRequiredException.class)
                .satisfies(ex -> assertThat(((PaymentRequiredException) ex).getCode())
                        .isEqualTo(PaymentRequiredCode.SEAT_LIMIT_EXCEEDED))
                .hasMessageContaining("PRO");
    }

    // U-SF123-02-03 : PRO 20 seats → pas de gate (Integer.MAX_VALUE)
    @Test
    void createInvitation_proPlan_noCap_succeeds() {
        User owner = buildUser("owner@example.com");
        Workspace workspace = buildWorkspace();
        WorkspaceMember ownerMember = buildMember(workspace, owner, "OWNER");
        List<WorkspaceMember> twentyMembers = new java.util.ArrayList<>();
        twentyMembers.add(ownerMember);
        for (int i = 0; i < 19; i++) twentyMembers.add(buildMember(workspace, buildUser("m" + i + "@ex.com"), "LAWYER"));

        setupAuth(owner);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(owner)).thenReturn(Optional.of(ownerMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), owner.getId())).thenReturn(Optional.of(ownerMember));
        when(workspaceInvitationRepository.existsByWorkspaceIdAndEmailAndStatus(workspace.getId(), "invitee@example.com", "PENDING")).thenReturn(false);
        when(workspaceMemberRepository.findByWorkspace_Id(workspace.getId())).thenReturn(twentyMembers);
        when(workspaceInvitationRepository.findByWorkspaceIdAndStatus(workspace.getId(), "PENDING")).thenReturn(List.of());
        when(planLimitService.getMaxSeatsForWorkspace(workspace.getId())).thenReturn(Integer.MAX_VALUE);
        when(workspaceInvitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceInvitationResponse r = service.createInvitation(
                new WorkspaceInvitationRequest("invitee@example.com", "LAWYER"),
                buildOidcUser("sub-owner", "owner@example.com"), "GOOGLE", null);

        assertThat(r.status()).isEqualTo("PENDING");
    }

    // U-SF123-02-04 : acceptInvitation → stripeSeatService.syncSeatCount appelé
    @Test
    void acceptInvitation_callsStripeSeatSync() {
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

        setupAuth(invitee);
        when(workspaceInvitationRepository.findByToken("valid-token")).thenReturn(Optional.of(invitation));
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(invitee)).thenReturn(Optional.of(existingPrimary));
        when(workspaceRepository.findById(targetWorkspace.getId())).thenReturn(Optional.of(targetWorkspace));
        when(workspaceMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workspaceInvitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.acceptInvitation(new AcceptInvitationRequest("valid-token"),
                buildOidcUser("sub-invitee", "invitee@example.com"), "GOOGLE", null);

        verify(stripeSeatService).syncSeatCount(targetWorkspace.getId());
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

    private void setupAuth(User user) {
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
    }

    private OidcUser buildOidcUser(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        return new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
    }
}
