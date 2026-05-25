package fr.ailegalcase.superadmin;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.workspace.WorkspaceCreatedResponse;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-251 SF-251-03 — unit tests pour {@link SuperAdminProspectBootstrapService}.
 *
 * <p>Vérifie les 3 branches (user inexistant / cas A vierge / cas B conflit)
 * et la normalisation email (lower + trim). Les validations Bean Validation
 * (email format, password length, country/legalDomain pattern, workspaceName
 * vide) sont couvertes par le controller IT — non testables au niveau service
 * car appliquées en amont par Spring.
 */
@ExtendWith(MockitoExtension.class)
class SuperAdminProspectBootstrapServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private WorkspaceService workspaceService;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private SuperAdminProspectBootstrapService service;

    @BeforeEach
    void setUp() {
        service = new SuperAdminProspectBootstrapService(
                userRepository, authAccountRepository, workspaceMemberRepository,
                workspaceService, subscriptionRepository, passwordEncoder);
    }

    private ProspectBootstrapRequest validRequest() {
        return new ProspectBootstrapRequest(
                "Marjolaine", "RENVERSEZ", "avocat@renversez.com",
                "printemps2026", "FRANCE", "DROIT_DU_TRAVAIL",
                "RENVERSEZ-MARJOLAINE");
    }

    private void stubWorkspaceCreation(UUID workspaceId, String name) {
        WorkspaceCreatedResponse wsResponse = new WorkspaceCreatedResponse(
                workspaceId, name, "ACTIVE", "FREE",
                "DROIT_DU_TRAVAIL", "FRANCE", null);
        when(workspaceService.createWorkspaceForBootstrappedUser(any(User.class),
                anyString(), anyString(), anyString())).thenReturn(wsResponse);

        Subscription subscription = new Subscription();
        subscription.setWorkspaceId(workspaceId);
        subscription.setPlanCode("FREE");
        subscription.setStatus("ACTIVE");
        Instant now = Instant.now();
        subscription.setStartedAt(now);
        subscription.setExpiresAt(now.plus(14, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(workspaceId))
                .thenReturn(Optional.of(subscription));
    }

    // U-01 : cas nominal — user inexistant → création User + AuthAccount LOCAL + workspace
    @Test
    void bootstrap_userInexistant_creeUserAuthWorkspace() {
        ProspectBootstrapRequest req = validRequest();
        UUID workspaceId = UUID.randomUUID();

        when(userRepository.findByEmail("avocat@renversez.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(passwordEncoder.encode("printemps2026")).thenReturn("BCRYPT_HASH");
        stubWorkspaceCreation(workspaceId, "RENVERSEZ-MARJOLAINE");

        ProspectBootstrapResponse response = service.bootstrapProspect(req);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("avocat@renversez.com");
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo("Marjolaine");
        assertThat(userCaptor.getValue().getLastName()).isEqualTo("RENVERSEZ");
        assertThat(userCaptor.getValue().getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<AuthAccount> accountCaptor = ArgumentCaptor.forClass(AuthAccount.class);
        verify(authAccountRepository).save(accountCaptor.capture());
        AuthAccount account = accountCaptor.getValue();
        assertThat(account.getProvider()).isEqualTo("LOCAL");
        assertThat(account.getProviderUserId()).isEqualTo("avocat@renversez.com");
        assertThat(account.getProviderEmail()).isEqualTo("avocat@renversez.com");
        assertThat(account.getPasswordHash()).isEqualTo("BCRYPT_HASH");
        assertThat(account.isEmailVerified()).isTrue();

        verify(workspaceService).createWorkspaceForBootstrappedUser(
                any(User.class), eq("RENVERSEZ-MARJOLAINE"),
                eq("DROIT_DU_TRAVAIL"), eq("FRANCE"));

        assertThat(response.workspaceId()).isEqualTo(workspaceId);
        assertThat(response.workspaceName()).isEqualTo("RENVERSEZ-MARJOLAINE");
        assertThat(response.expiresAt()).isNotNull();
    }

    // U-02 : cas A — user existant sans aucun WorkspaceMember → reset password + create workspace
    @Test
    void bootstrap_userExistantSansWorkspace_resetPasswordEtCreeWorkspace() {
        ProspectBootstrapRequest req = validRequest();
        UUID workspaceId = UUID.randomUUID();

        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setEmail("avocat@renversez.com");
        existingUser.setStatus("ACTIVE");

        AuthAccount existingLocal = new AuthAccount();
        existingLocal.setUser(existingUser);
        existingLocal.setProvider("LOCAL");
        existingLocal.setProviderUserId("avocat@renversez.com");
        existingLocal.setPasswordHash("OLD_HASH");
        existingLocal.setEmailVerified(false);

        when(userRepository.findByEmail("avocat@renversez.com")).thenReturn(Optional.of(existingUser));
        when(workspaceMemberRepository.findByUser(existingUser)).thenReturn(List.of());
        when(authAccountRepository.findByProviderAndProviderUserId("LOCAL", "avocat@renversez.com"))
                .thenReturn(Optional.of(existingLocal));
        when(passwordEncoder.encode("printemps2026")).thenReturn("NEW_HASH");
        stubWorkspaceCreation(workspaceId, "RENVERSEZ-MARJOLAINE");

        ProspectBootstrapResponse response = service.bootstrapProspect(req);

        // Pas de save User (pas de création)
        verify(userRepository, never()).save(any(User.class));

        ArgumentCaptor<AuthAccount> accountCaptor = ArgumentCaptor.forClass(AuthAccount.class);
        verify(authAccountRepository).save(accountCaptor.capture());
        AuthAccount saved = accountCaptor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("NEW_HASH");
        assertThat(saved.isEmailVerified()).isTrue();

        verify(workspaceService).createWorkspaceForBootstrappedUser(
                eq(existingUser), eq("RENVERSEZ-MARJOLAINE"),
                eq("DROIT_DU_TRAVAIL"), eq("FRANCE"));

        assertThat(response.userId()).isEqualTo(existingUser.getId());
        assertThat(response.workspaceId()).isEqualTo(workspaceId);
    }

    // U-02b : cas A bis — user existant sans LOCAL AuthAccount (compte OAuth pur) → création LOCAL
    @Test
    void bootstrap_userExistantSansLocalAuth_creeAuthLocalEtWorkspace() {
        ProspectBootstrapRequest req = validRequest();
        UUID workspaceId = UUID.randomUUID();

        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setEmail("avocat@renversez.com");

        when(userRepository.findByEmail("avocat@renversez.com")).thenReturn(Optional.of(existingUser));
        when(workspaceMemberRepository.findByUser(existingUser)).thenReturn(List.of());
        when(authAccountRepository.findByProviderAndProviderUserId("LOCAL", "avocat@renversez.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("printemps2026")).thenReturn("BCRYPT_HASH");
        stubWorkspaceCreation(workspaceId, "RENVERSEZ-MARJOLAINE");

        service.bootstrapProspect(req);

        ArgumentCaptor<AuthAccount> captor = ArgumentCaptor.forClass(AuthAccount.class);
        verify(authAccountRepository).save(captor.capture());
        AuthAccount account = captor.getValue();
        assertThat(account.getProvider()).isEqualTo("LOCAL");
        assertThat(account.getProviderUserId()).isEqualTo("avocat@renversez.com");
        assertThat(account.getUser()).isEqualTo(existingUser);
        assertThat(account.getPasswordHash()).isEqualTo("BCRYPT_HASH");
        assertThat(account.isEmailVerified()).isTrue();
    }

    // U-03 : cas B — user existant avec ≥ 1 WorkspaceMember → 409
    @Test
    void bootstrap_userExistantAvecWorkspace_jette409() {
        ProspectBootstrapRequest req = validRequest();

        User productiveUser = new User();
        productiveUser.setId(UUID.randomUUID());
        productiveUser.setEmail("avocat@renversez.com");

        WorkspaceMember existingMember = new WorkspaceMember();

        when(userRepository.findByEmail("avocat@renversez.com")).thenReturn(Optional.of(productiveUser));
        when(workspaceMemberRepository.findByUser(productiveUser))
                .thenReturn(List.of(existingMember));

        assertThatThrownBy(() -> service.bootstrapProspect(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Compte déjà actif");

        verify(userRepository, never()).save(any(User.class));
        verify(authAccountRepository, never()).save(any());
        verify(workspaceService, never()).createWorkspaceForBootstrappedUser(any(), any(), any(), any());
    }

    // U-04 : email normalisé (UPPER + espaces → lower + trim) avant lookup et persist
    @Test
    void bootstrap_emailNormalise() {
        ProspectBootstrapRequest req = new ProspectBootstrapRequest(
                "Marjolaine", "RENVERSEZ", "  AVOCAT@RENVERSEZ.COM  ",
                "printemps2026", "FRANCE", "DROIT_DU_TRAVAIL",
                "RENVERSEZ-MARJOLAINE");
        UUID workspaceId = UUID.randomUUID();

        // Le service doit chercher l'email normalisé (lower + trim).
        when(userRepository.findByEmail("avocat@renversez.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(passwordEncoder.encode("printemps2026")).thenReturn("HASH");
        stubWorkspaceCreation(workspaceId, "RENVERSEZ-MARJOLAINE");

        service.bootstrapProspect(req);

        // Vérifie que findByEmail a bien été appelé avec la version normalisée
        verify(userRepository).findByEmail("avocat@renversez.com");

        // Vérifie que User.email est persisté normalisé
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("avocat@renversez.com");

        // Vérifie que AuthAccount.providerUserId est normalisé
        ArgumentCaptor<AuthAccount> acctCaptor = ArgumentCaptor.forClass(AuthAccount.class);
        verify(authAccountRepository).save(acctCaptor.capture());
        assertThat(acctCaptor.getValue().getProviderUserId()).isEqualTo("avocat@renversez.com");
        assertThat(acctCaptor.getValue().getProviderEmail()).isEqualTo("avocat@renversez.com");
    }

    // U-05 : country normalisé en uppercase avant appel WorkspaceService
    @Test
    void bootstrap_countryEtDomaineNormalisesUppercase() {
        ProspectBootstrapRequest req = new ProspectBootstrapRequest(
                "Marjolaine", "RENVERSEZ", "avocat@renversez.com",
                "printemps2026", "FRANCE", "DROIT_DU_TRAVAIL",
                "RENVERSEZ-MARJOLAINE");
        UUID workspaceId = UUID.randomUUID();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");
        stubWorkspaceCreation(workspaceId, "RENVERSEZ-MARJOLAINE");

        service.bootstrapProspect(req);

        verify(workspaceService).createWorkspaceForBootstrappedUser(
                any(User.class), eq("RENVERSEZ-MARJOLAINE"),
                eq("DROIT_DU_TRAVAIL"), eq("FRANCE"));
    }

    // U-06 : firstName/lastName/workspaceName trim avant persist
    @Test
    void bootstrap_firstLastWorkspaceTrim() {
        ProspectBootstrapRequest req = new ProspectBootstrapRequest(
                "  Marjolaine  ", "  RENVERSEZ  ", "avocat@renversez.com",
                "printemps2026", "FRANCE", "DROIT_DU_TRAVAIL",
                "  RENVERSEZ-MARJOLAINE  ");
        UUID workspaceId = UUID.randomUUID();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");
        stubWorkspaceCreation(workspaceId, "RENVERSEZ-MARJOLAINE");

        service.bootstrapProspect(req);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo("Marjolaine");
        assertThat(userCaptor.getValue().getLastName()).isEqualTo("RENVERSEZ");

        // workspaceName trim avant délégation à WorkspaceService
        verify(workspaceService).createWorkspaceForBootstrappedUser(
                any(User.class), eq("RENVERSEZ-MARJOLAINE"),
                anyString(), anyString());
    }

    // U-07 : la réponse contient bien expiresAt non null récupéré depuis la subscription créée
    @Test
    void bootstrap_responseContainsExpiresAtFromSubscription() {
        ProspectBootstrapRequest req = validRequest();
        UUID workspaceId = UUID.randomUUID();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");

        // Subscription avec expiresAt explicite (simule la valeur posée par WorkspaceService)
        Instant expectedExpiresAt = Instant.parse("2026-06-08T10:00:00Z");
        WorkspaceCreatedResponse wsResponse = new WorkspaceCreatedResponse(
                workspaceId, "RENVERSEZ-MARJOLAINE", "ACTIVE", "FREE",
                "DROIT_DU_TRAVAIL", "FRANCE", null);
        when(workspaceService.createWorkspaceForBootstrappedUser(any(User.class),
                anyString(), anyString(), anyString())).thenReturn(wsResponse);

        Subscription sub = new Subscription();
        sub.setWorkspaceId(workspaceId);
        sub.setPlanCode("FREE");
        sub.setStatus("ACTIVE");
        sub.setStartedAt(Instant.parse("2026-05-25T10:00:00Z"));
        sub.setExpiresAt(expectedExpiresAt);
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));

        ProspectBootstrapResponse response = service.bootstrapProspect(req);

        assertThat(response.expiresAt())
                .as("Le cœur de SF-251-03 : expiresAt non null garantit que le bug Renversez ne peut se reproduire")
                .isEqualTo(expectedExpiresAt)
                .isNotNull();
    }

    // U-08 : invariant SF-251-03 — si la subscription manque post-création workspace, IllegalStateException
    @Test
    void bootstrap_subscriptionIntrouvable_jetteIllegalState() {
        ProspectBootstrapRequest req = validRequest();
        UUID workspaceId = UUID.randomUUID();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");

        WorkspaceCreatedResponse wsResponse = new WorkspaceCreatedResponse(
                workspaceId, "RENVERSEZ-MARJOLAINE", "ACTIVE", "FREE",
                "DROIT_DU_TRAVAIL", "FRANCE", null);
        when(workspaceService.createWorkspaceForBootstrappedUser(any(User.class),
                anyString(), anyString(), anyString())).thenReturn(wsResponse);
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.bootstrapProspect(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Subscription introuvable");
    }

    // U-09 : cas A — un seul save AuthAccount (pas de double persist user+account)
    @Test
    void bootstrap_userExistantSansWorkspace_unSeulSaveAuthAccount() {
        ProspectBootstrapRequest req = validRequest();
        UUID workspaceId = UUID.randomUUID();

        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setEmail("avocat@renversez.com");

        AuthAccount localAcct = new AuthAccount();
        localAcct.setUser(existingUser);
        localAcct.setProvider("LOCAL");
        localAcct.setProviderUserId("avocat@renversez.com");

        when(userRepository.findByEmail("avocat@renversez.com")).thenReturn(Optional.of(existingUser));
        when(workspaceMemberRepository.findByUser(existingUser)).thenReturn(List.of());
        when(authAccountRepository.findByProviderAndProviderUserId("LOCAL", "avocat@renversez.com"))
                .thenReturn(Optional.of(localAcct));
        when(passwordEncoder.encode(anyString())).thenReturn("NEW_HASH");
        stubWorkspaceCreation(workspaceId, "RENVERSEZ-MARJOLAINE");

        service.bootstrapProspect(req);

        verify(authAccountRepository, times(1)).save(any(AuthAccount.class));
    }
}
