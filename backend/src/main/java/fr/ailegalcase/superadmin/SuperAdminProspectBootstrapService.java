package fr.ailegalcase.superadmin;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.workspace.WorkspaceCreatedResponse;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * F-251 SF-251-03 — orchestration de la création d'un compte prospect côté
 * super-admin, en remplacement de la chaîne d'{@code INSERT} SQL directs de
 * la skill {@code prospect-account-bootstrap} (étape 4).
 *
 * <p>Passe entièrement par JPA → bénéficie automatiquement du
 * {@code @PrePersist} SF-251-02 sur {@link Subscription}, donc le compte
 * créé a toujours un {@code expires_at} non NULL — élimine la possibilité
 * de recréer le bug Renversez 13/05 via ce chemin.
 *
 * <p>Trois cas gérés :
 * <ul>
 *   <li><b>User inexistant</b> → création User + AuthAccount LOCAL +
 *       workspace + membership + subscription.</li>
 *   <li><b>User existant sans aucun WorkspaceMember</b> (cas A skill —
 *       compte vierge créé via OAuth sans onboarding terminé) → reset du
 *       password sur l'AuthAccount LOCAL (créé si absent), création du
 *       workspace + membership + subscription.</li>
 *   <li><b>User existant avec ≥ 1 WorkspaceMember</b> (cas B skill —
 *       compte productif) → {@code 409 Conflict}, l'opérateur doit
 *       analyser avant d'agir.</li>
 * </ul>
 */
@Service
public class SuperAdminProspectBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminProspectBootstrapService.class);

    private final UserRepository userRepository;
    private final AuthAccountRepository authAccountRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceService workspaceService;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminProspectBootstrapService(UserRepository userRepository,
                                              AuthAccountRepository authAccountRepository,
                                              WorkspaceMemberRepository workspaceMemberRepository,
                                              WorkspaceService workspaceService,
                                              SubscriptionRepository subscriptionRepository,
                                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authAccountRepository = authAccountRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceService = workspaceService;
        this.subscriptionRepository = subscriptionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ProspectBootstrapResponse bootstrapProspect(ProspectBootstrapRequest req) {
        String email = req.email().toLowerCase().trim();
        String firstName = req.firstName().trim();
        String lastName = req.lastName().trim();
        String workspaceName = req.workspaceName().trim();
        String country = req.country().toUpperCase();
        String legalDomain = req.legalDomain().toUpperCase();

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            // Cas B — compte productif → 409.
            if (!workspaceMemberRepository.findByUser(user).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Compte déjà actif — ne pas bootstrap");
            }
            // Cas A — compte vierge (OAuth sans onboarding ou bootstrap partiel)
            // → reset password sur l'AuthAccount LOCAL (création si absent).
            resetOrCreateLocalAccount(user, email, req.password());
        } else {
            // Cas nominal — user inexistant → création complète.
            user = createUserWithLocalAuth(email, firstName, lastName, req.password());
        }

        WorkspaceCreatedResponse workspace = workspaceService.createWorkspaceForBootstrappedUser(
                user, workspaceName, legalDomain, country);

        Subscription subscription = subscriptionRepository.findByWorkspaceId(workspace.workspaceId())
                .orElseThrow(() -> new IllegalStateException(
                        "Subscription introuvable pour workspace " + workspace.workspaceId()
                                + " — invariant SF-251-03 violé"));

        log.info("F-251 SF-251-03 — prospect bootstrap réussi : user={} workspace={} expiresAt={}",
                user.getId(), workspace.workspaceId(), subscription.getExpiresAt());

        return new ProspectBootstrapResponse(
                user.getId(),
                workspace.workspaceId(),
                workspace.name(),
                subscription.getExpiresAt()
        );
    }

    private User createUserWithLocalAuth(String email, String firstName, String lastName, String rawPassword) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus("ACTIVE");
        user = userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("LOCAL");
        account.setProviderUserId(email);
        account.setProviderEmail(email);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setEmailVerified(true);
        authAccountRepository.save(account);

        return user;
    }

    private void resetOrCreateLocalAccount(User user, String email, String rawPassword) {
        AuthAccount localAccount = authAccountRepository
                .findByProviderAndProviderUserId("LOCAL", email)
                .orElse(null);

        if (localAccount == null) {
            // User exists (e.g. créé via OAuth) mais aucun compte LOCAL — création.
            localAccount = new AuthAccount();
            localAccount.setUser(user);
            localAccount.setProvider("LOCAL");
            localAccount.setProviderUserId(email);
            localAccount.setProviderEmail(email);
        }
        localAccount.setPasswordHash(passwordEncoder.encode(rawPassword));
        localAccount.setEmailVerified(true);
        authAccountRepository.save(localAccount);
    }
}
