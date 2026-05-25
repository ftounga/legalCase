package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.billing.StripeCheckoutService;
import fr.ailegalcase.billing.StripeCustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceService.class);

    /**
     * SF-156-01 : plans autorisés à créer un workspace supplémentaire.
     * Un OWNER FREE ou SOLO ne peut pas — il doit d'abord upgrader.
     */
    private static final Set<String> PLANS_ALLOWED_TO_CREATE_WORKSPACE = Set.of("TEAM", "PRO");

    /** SF-156-01 : plans autorisés comme cible d'un nouveau workspace. */
    private static final Set<String> NEW_WORKSPACE_TARGET_PLANS = Set.of("TEAM", "PRO");

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final AuthAccountRepository authAccountRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final StripeCustomerService stripeCustomerService;
    private final StripeCheckoutService stripeCheckoutService;
    private final PlanLimitService planLimitService;
    private final EmailService emailService;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository workspaceMemberRepository,
                            AuthAccountRepository authAccountRepository,
                            SubscriptionRepository subscriptionRepository,
                            StripeCustomerService stripeCustomerService,
                            StripeCheckoutService stripeCheckoutService,
                            PlanLimitService planLimitService,
                            EmailService emailService) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.authAccountRepository = authAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.stripeCustomerService = stripeCustomerService;
        this.stripeCheckoutService = stripeCheckoutService;
        this.planLimitService = planLimitService;
        this.emailService = emailService;
    }

    /**
     * SF-156-01 : création d'un workspace.
     *
     * <p>Deux modes :
     * <ul>
     *   <li><strong>Premier workspace de l'utilisateur</strong> (onboarding,
     *       aucun membership existant) — créé directement en {@code FREE}
     *       / {@code ACTIVE} comme avant (rétrocompatibilité onboarding F-08).
     *       Le champ {@code plan} de la requête est ignoré.</li>
     *   <li><strong>Workspace supplémentaire</strong> (l'utilisateur est déjà
     *       membre d'au moins un workspace) — gate plan : l'OWNER courant doit
     *       être en TEAM ou PRO actif ; sinon {@code 403}. Le {@code plan}
     *       choisi (TEAM ou PRO uniquement) est appliqué au nouveau workspace
     *       qui est créé en {@code PENDING_PAYMENT} avec une session Stripe
     *       Checkout retournée pour activation.</li>
     * </ul>
     *
     * <p>Rollback transactionnel : si Stripe échoue (502/503), l'exception
     * remonte et la transaction est rollback — aucun workspace n'est persisté.
     */
    @Transactional
    public WorkspaceCreatedResponse createWorkspace(OidcUser oidcUser,
                                                    String provider,
                                                    String name,
                                                    String legalDomain,
                                                    String country,
                                                    String plan,
                                                    Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);

        boolean isFirstWorkspace = !workspaceMemberRepository.existsByUser(user);

        if (isFirstWorkspace) {
            return createFirstWorkspace(user, name, legalDomain, country);
        }

        // Workspace supplémentaire (SF-156-01) — gate plan + PENDING_PAYMENT.
        return createAdditionalPendingPaymentWorkspace(user, name, legalDomain, country, plan);
    }

    /**
     * F-251 SF-251-03 — provisionnement du workspace d'un utilisateur via le
     * chemin JPA, sans envoyer le mail "bienvenue onboarding" F-73.
     *
     * <p>Crée workspace {@code ACTIVE} / {@code FREE}, membership {@code OWNER}
     * primary, subscription {@code FREE} active sur 14 jours (le hook
     * {@code @PrePersist} SF-251-02 garantit {@code expiresAt} même si oublié),
     * et tente la création d'un customer Stripe en best-effort.
     *
     * <p>Méthode publique pour le bootstrap super-admin
     * ({@code SuperAdminProspectBootstrapService}) — la SF-251-03 a remplacé
     * la chaîne d'{@code INSERT} SQL de la skill {@code prospect-account-bootstrap}
     * par cet appel JPA, éliminant le risque de Subscription FREE sans
     * {@code expires_at}.
     *
     * <p>Le mail "bienvenue onboarding" est volontairement absent : l'opérateur
     * super-admin envoie ensuite son propre mail personnalisé (skill étape 6,
     * avec identifiants + créneaux RDV). Le chemin nominal
     * ({@link #createFirstWorkspace}) ajoute ce mail au-dessus de cette méthode.
     */
    public WorkspaceCreatedResponse createWorkspaceForBootstrappedUser(User user, String name,
                                                                       String legalDomain, String country) {
        Workspace workspace = persistWorkspace(user, name, legalDomain, country, "FREE", WorkspaceStatus.ACTIVE);
        persistOwnerMembership(user, workspace, true);

        Instant now = Instant.now();
        Subscription subscription = new Subscription();
        subscription.setWorkspaceId(workspace.getId());
        subscription.setPlanCode("FREE");
        subscription.setStatus("ACTIVE");
        subscription.setStartedAt(now);
        subscription.setExpiresAt(now.plus(14, ChronoUnit.DAYS));
        subscriptionRepository.save(subscription);

        stripeCustomerService.createCustomer(user.getEmail(), workspace.getId())
                .ifPresent(customerId -> {
                    subscription.setStripeCustomerId(customerId);
                    subscriptionRepository.save(subscription);
                });

        return new WorkspaceCreatedResponse(workspace.getId(), workspace.getName(),
                workspace.getStatus(), workspace.getPlanCode(),
                workspace.getLegalDomain(), workspace.getCountry(), null);
    }

    /**
     * Chemin nominal d'onboarding (1er workspace via {@link #createWorkspace}) :
     * délègue à {@link #createWorkspaceForBootstrappedUser} puis envoie le mail
     * "bienvenue onboarding" F-73.
     *
     * <p>F-154 : email "bienvenue onboarding" uniquement à la création du 1er
     * workspace ; les workspaces additionnels ne déclenchent pas de mail pour
     * éviter le spam. Le bootstrap super-admin (SF-251-03) n'envoie pas non
     * plus ce mail — l'opérateur envoie son propre mail personnalisé.
     */
    private WorkspaceCreatedResponse createFirstWorkspace(User user, String name,
                                                          String legalDomain, String country) {
        WorkspaceCreatedResponse response = createWorkspaceForBootstrappedUser(user, name, legalDomain, country);

        try {
            emailService.sendOnboardingWelcome(user);
        } catch (Exception e) {
            log.warn("Failed to send onboarding welcome to {} — {}", user.getEmail(), e.getMessage());
        }

        return response;
    }

    private WorkspaceCreatedResponse createAdditionalPendingPaymentWorkspace(User user, String name,
                                                                             String legalDomain,
                                                                             String country, String plan) {
        // Gate validation : plan obligatoire et restreint à TEAM/PRO.
        // (la validation @Pattern du DTO refuse déjà null/FREE/SOLO/INVALID, mais
        // on garde le check serveur car le service est appelable depuis d'autres
        // chemins futurs — défense en profondeur.)
        if (plan == null || !NEW_WORKSPACE_TARGET_PLANS.contains(plan)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Plan invalide — seuls TEAM et PRO sont acceptés pour un workspace supplémentaire");
        }

        // Gate plan : l'OWNER doit être TEAM ou PRO actif sur son workspace
        // courant (primary).
        WorkspaceMember currentMembership = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseGet(() -> {
                    List<WorkspaceMember> all = workspaceMemberRepository.findByUser(user);
                    return all.isEmpty() ? null : all.get(0);
                });
        if (currentMembership == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace courant introuvable");
        }
        if (!"OWNER".equals(currentMembership.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul l'OWNER d'un workspace peut créer un workspace supplémentaire");
        }
        UUID currentWorkspaceId = currentMembership.getWorkspace().getId();
        String currentPlan = planLimitService.getPlanCodeForWorkspace(currentWorkspaceId);
        if (!PLANS_ALLOWED_TO_CREATE_WORKSPACE.contains(currentPlan)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La création d'un workspace supplémentaire nécessite un abonnement TEAM ou PRO");
        }

        // Persistance du nouveau workspace en PENDING_PAYMENT.
        // Le plan est pré-positionné (TEAM ou PRO) pour cohérence UI ; le
        // webhook customer.subscription.created confirmera en passant
        // status à ACTIVE.
        Workspace workspace = persistWorkspace(user, name, legalDomain, country, plan, WorkspaceStatus.PENDING_PAYMENT);
        persistOwnerMembership(user, workspace, false); // jamais primary à la création (CA9 isolation)

        // Pré-création de l'enregistrement Subscription (status PENDING_PAYMENT)
        // pour permettre au webhook de retrouver la ligne par workspaceId
        // ou par stripeCustomerId (selon ce qui arrive d'abord). Le planCode
        // local est figé à la valeur choisie ; status reste PENDING_PAYMENT
        // tant que Stripe n'a pas confirmé.
        Instant now = Instant.now();
        Subscription subscription = new Subscription();
        subscription.setWorkspaceId(workspace.getId());
        subscription.setPlanCode(plan);
        subscription.setStatus("PENDING_PAYMENT");
        subscription.setStartedAt(now);
        subscriptionRepository.save(subscription);

        // Appel Stripe. Échec → exception 502 → rollback transactionnel (CA10).
        String checkoutUrl;
        try {
            Optional<String> sessionUrl = stripeCheckoutService
                    .createSubscriptionSessionForNewWorkspace(plan, user.getEmail(), workspace.getId());
            if (sessionUrl.isEmpty()) {
                // Stripe désactivé (dev / test local) — on active immédiatement
                // pour ne pas bloquer le dev. En prod stripeEnabled=true.
                log.info("Stripe désactivé — workspace {} activé directement", workspace.getId());
                workspace.setStatus(WorkspaceStatus.ACTIVE);
                workspaceRepository.save(workspace);
                subscription.setStatus("ACTIVE");
                subscriptionRepository.save(subscription);
                checkoutUrl = null;
            } else {
                checkoutUrl = sessionUrl.get();
            }
        } catch (ResponseStatusException e) {
            // Laissé remonter — la transaction rollback automatiquement.
            log.error("Stripe Checkout failed for new workspace {} — rollback: {}",
                    workspace.getId(), e.getReason());
            throw e;
        }

        return new WorkspaceCreatedResponse(workspace.getId(), workspace.getName(),
                workspace.getStatus(), workspace.getPlanCode(),
                workspace.getLegalDomain(), workspace.getCountry(), checkoutUrl);
    }

    private Workspace persistWorkspace(User user, String name, String legalDomain,
                                       String country, String planCode, String status) {
        Workspace workspace = new Workspace();
        workspace.setName(name.strip().toUpperCase());
        workspace.setSlug(UUID.randomUUID().toString());
        workspace.setOwner(user);
        workspace.setLegalDomain(legalDomain);
        workspace.setCountry(country);
        workspace.setPlanCode(planCode);
        workspace.setStatus(status);
        return workspaceRepository.save(workspace);
    }

    private void persistOwnerMembership(User user, Workspace workspace, boolean primary) {
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(primary);
        workspaceMemberRepository.save(member);
    }

    @Transactional
    public void createDefaultWorkspace(User user) {
        if (workspaceMemberRepository.existsByUser(user)) {
            return;
        }

        Workspace workspace = persistWorkspace(user, user.getEmail(), "DROIT_DU_TRAVAIL",
                "FRANCE", "FREE", WorkspaceStatus.ACTIVE);
        persistOwnerMembership(user, workspace, true);

        Instant now = Instant.now();
        Subscription subscription = new Subscription();
        subscription.setWorkspaceId(workspace.getId());
        subscription.setPlanCode("FREE");
        subscription.setStatus("ACTIVE");
        subscription.setStartedAt(now);
        subscription.setExpiresAt(now.plus(14, ChronoUnit.DAYS));
        subscriptionRepository.save(subscription);

        stripeCustomerService.createCustomer(user.getEmail(), workspace.getId())
                .ifPresent(customerId -> {
                    subscription.setStripeCustomerId(customerId);
                    subscriptionRepository.save(subscription);
                });
    }

    private User resolveUser(OidcUser oidcUser, String provider, Principal principal) {
        if (oidcUser != null) {
            return authAccountRepository
                    .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                    .getUser();
        }
        // Auth locale : principal.getName() = email
        return authAccountRepository
                .findByProviderAndProviderUserId("LOCAL", principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide"))
                .getUser();
    }

    @Transactional
    public WorkspaceResponse getCurrentWorkspace(OidcUser oidcUser, String provider, Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);

        WorkspaceMember member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseGet(() -> {
                    List<WorkspaceMember> members = workspaceMemberRepository.findByUser(user);
                    if (members.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found");
                    WorkspaceMember fallback = members.get(0);
                    fallback.setPrimary(true);
                    return workspaceMemberRepository.save(fallback);
                });
        Workspace workspace = member.getWorkspace();

        Subscription sub = subscriptionRepository.findByWorkspaceId(workspace.getId()).orElse(null);
        String planCode = sub != null ? sub.getPlanCode() : workspace.getPlanCode();
        Instant expiresAt = sub != null ? sub.getExpiresAt() : null;

        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getSlug(),
                planCode, workspace.getStatus(), expiresAt, true,
                workspace.getLegalDomain(), workspace.getCountry());
    }

    /**
     * SF-156-01 : liste des workspaces de l'utilisateur.
     *
     * <p>Les workspaces en {@code CANCELLED} sont exclus (workspaces supprimés
     * suite à un paiement échoué / timeout 24 h, conservés temporairement pour
     * audit avant cleanup définitif). Les workspaces en {@code PENDING_PAYMENT}
     * restent visibles — le frontend SF-156-02 affiche une banderole et
     * désactive les actions d'écriture (invariant SF-156-00 §1).
     */
    @Transactional(readOnly = true)
    public java.util.List<WorkspaceResponse> listUserWorkspaces(OidcUser oidcUser, String provider, Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);

        return workspaceMemberRepository.findByUser(user).stream()
                .filter(member -> !WorkspaceStatus.CANCELLED.equals(member.getWorkspace().getStatus()))
                .map(member -> {
                    Workspace ws = member.getWorkspace();
                    Subscription sub = subscriptionRepository.findByWorkspaceId(ws.getId()).orElse(null);
                    String planCode = sub != null ? sub.getPlanCode() : ws.getPlanCode();
                    Instant expiresAt = sub != null ? sub.getExpiresAt() : null;
                    return new WorkspaceResponse(ws.getId(), ws.getName(), ws.getSlug(),
                            planCode, ws.getStatus(), expiresAt, member.isPrimary(),
                            ws.getLegalDomain(), ws.getCountry());
                })
                .toList();
    }

    /**
     * SF-156-01 : un switch vers un workspace {@code PENDING_PAYMENT} est
     * autorisé (l'avocat peut le consulter pour réactiver le paiement) mais
     * il n'est pas marqué primary tant qu'il n'est pas activé. Un workspace
     * {@code CANCELLED} est inaccessible.
     */
    @Transactional
    public WorkspaceResponse switchWorkspace(OidcUser oidcUser, String provider, UUID targetWorkspaceId, Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);

        WorkspaceMember target = workspaceMemberRepository
                .findByWorkspace_IdAndUser_Id(targetWorkspaceId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this workspace"));

        Workspace ws = target.getWorkspace();
        if (WorkspaceStatus.CANCELLED.equals(ws.getStatus())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Workspace supprimé");
        }

        workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .ifPresent(current -> {
                    current.setPrimary(false);
                    workspaceMemberRepository.save(current);
                });

        target.setPrimary(true);
        workspaceMemberRepository.save(target);

        Instant expiresAt = subscriptionRepository.findByWorkspaceId(ws.getId())
                .map(Subscription::getExpiresAt).orElse(null);

        return new WorkspaceResponse(ws.getId(), ws.getName(), ws.getSlug(),
                ws.getPlanCode(), ws.getStatus(), expiresAt, true,
                ws.getLegalDomain(), ws.getCountry());
    }
}
