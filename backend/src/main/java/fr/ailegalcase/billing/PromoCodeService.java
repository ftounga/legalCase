package fr.ailegalcase.billing;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.superadmin.SuperAdminService;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * F-255 SF-255-01 — logique métier des codes promo / coupons partenaires.
 *
 * <p>Trois opérations administratives (super-admin) — création, listing,
 * désactivation — et une opération utilisateur — redemption TRIAL_EXTENSION.
 *
 * <p>La redemption STRIPE_DISCOUNT renvoie 409 PROMO_CODE_TYPE_NOT_SUPPORTED_YET
 * en V1 ; SF-255-04 implémentera l'intégration Stripe Coupons.
 *
 * <p>Atomicité : la redemption combine 3 opérations (incrément {@code uses_count},
 * extension {@code subscriptions.expires_at}, INSERT redemption) dans une seule
 * transaction. L'incrément utilise un {@code @Modifying Query} avec garde-fou
 * {@code uses_count < max_uses} — pas de SELECT FOR UPDATE, donc portable
 * H2/PostgreSQL.
 */
@Service
public class PromoCodeService {

    private static final Logger log = LoggerFactory.getLogger(PromoCodeService.class);

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeRedemptionRepository redemptionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final SuperAdminService superAdminService;
    private final CurrentUserResolver currentUserResolver;

    public PromoCodeService(PromoCodeRepository promoCodeRepository,
                            PromoCodeRedemptionRepository redemptionRepository,
                            SubscriptionRepository subscriptionRepository,
                            WorkspaceMemberRepository workspaceMemberRepository,
                            SuperAdminService superAdminService,
                            CurrentUserResolver currentUserResolver) {
        this.promoCodeRepository = promoCodeRepository;
        this.redemptionRepository = redemptionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.superAdminService = superAdminService;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional
    public PromoCodeDto createCode(PromoCodeCreateRequest request,
                                   OidcUser oidcUser,
                                   String provider) {
        User admin = assertSuperAdmin(oidcUser, provider);
        validateTypeValueDays(request.type(), request.valueDays());
        validateExpiresAtFuture(request.expiresAt());

        String normalized = normalize(request.code());

        promoCodeRepository.findByCodeIgnoreCase(normalized).ifPresent(existing -> {
            throw new PromoCodeException(PromoCodeErrorCode.PROMO_CODE_DUPLICATE,
                    "Promo code " + normalized + " already exists");
        });

        PromoCode entity = new PromoCode();
        entity.setCode(normalized);
        entity.setType(request.type());
        entity.setValueDays(request.valueDays());
        entity.setPartnerLabel(request.partnerLabel().trim());
        entity.setMaxUses(request.maxUses());
        entity.setUsesCount(0);
        entity.setExpiresAt(request.expiresAt());
        entity.setActive(true);
        entity.setCreatedByUserId(admin.getId());

        PromoCode saved = promoCodeRepository.save(entity);
        log.info("PromoCode action=CREATE id={} code={} type={} partner={} maxUses={} expiresAt={}",
                saved.getId(), saved.getCode(), saved.getType(), saved.getPartnerLabel(),
                saved.getMaxUses(), saved.getExpiresAt());
        return PromoCodeDto.from(saved, 0L);
    }

    @Transactional(readOnly = true)
    public List<PromoCodeDto> listCodes(OidcUser oidcUser, String provider) {
        assertSuperAdmin(oidcUser, provider);
        List<PromoCode> codes = promoCodeRepository.findAllByOrderByCreatedAtDesc();
        return codes.stream()
                .map(code -> PromoCodeDto.from(code,
                        redemptionRepository.countByPromoCodeId(code.getId())))
                .toList();
    }

    @Transactional
    public PromoCodeDto deactivateCode(UUID id, OidcUser oidcUser, String provider) {
        assertSuperAdmin(oidcUser, provider);
        PromoCode code = promoCodeRepository.findById(id)
                .orElseThrow(() -> new PromoCodeException(PromoCodeErrorCode.PROMO_CODE_NOT_FOUND,
                        "Promo code " + id + " not found"));

        if (code.isActive()) {
            code.setActive(false);
            promoCodeRepository.save(code);
            log.info("PromoCode action=DEACTIVATE id={} code={}", code.getId(), code.getCode());
        } else {
            log.info("PromoCode action=DEACTIVATE id={} code={} (already inactive, idempotent)",
                    code.getId(), code.getCode());
        }
        long usesCount = redemptionRepository.countByPromoCodeId(code.getId());
        return PromoCodeDto.from(code, usesCount);
    }

    @Transactional
    public RedeemResponse redeemTrialExtension(UUID workspaceId,
                                               RedeemRequest request,
                                               OidcUser oidcUser,
                                               String provider,
                                               Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        assertWorkspaceMembership(workspaceId, user);

        String normalized = normalize(request.code());
        PromoCode code = promoCodeRepository.findByCodeIgnoreCase(normalized)
                .orElseThrow(() -> new PromoCodeException(PromoCodeErrorCode.PROMO_CODE_NOT_FOUND,
                        "Promo code " + normalized + " not found"));

        if (code.getType() != PromoCodeType.TRIAL_EXTENSION) {
            throw new PromoCodeException(PromoCodeErrorCode.PROMO_CODE_TYPE_NOT_SUPPORTED_YET,
                    "Promo code type " + code.getType() + " is not supported yet");
        }
        if (!code.isActive()) {
            throw new PromoCodeException(PromoCodeErrorCode.PROMO_CODE_INACTIVE,
                    "Promo code " + normalized + " is inactive");
        }
        Instant now = Instant.now();
        if (!code.getExpiresAt().isAfter(now)) {
            throw new PromoCodeException(PromoCodeErrorCode.PROMO_CODE_EXPIRED,
                    "Promo code " + normalized + " has expired");
        }
        if (code.getValueDays() == null || code.getValueDays() < 1) {
            // Garde-fou : un TRIAL_EXTENSION sans valueDays valide ne devrait pas
            // exister (validation à la création), mais on coupe court ici plutôt
            // que d'écrire une extension de 0/NULL jour.
            throw new PromoCodeException(PromoCodeErrorCode.PROMO_CODE_TYPE_NOT_SUPPORTED_YET,
                    "Promo code " + normalized + " has no valueDays");
        }

        assertWorkspaceCanRedeemTrialExtension(workspaceId);

        Subscription subscription = subscriptionRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new PromoCodeException(PromoCodeErrorCode.WORKSPACE_NOT_TRIALING,
                        "No subscription found for workspace " + workspaceId));
        if (!"FREE".equals(subscription.getPlanCode())
                || subscription.getExpiresAt() == null
                || !subscription.getExpiresAt().isAfter(now)) {
            throw new PromoCodeException(PromoCodeErrorCode.WORKSPACE_NOT_TRIALING,
                    "Workspace " + workspaceId + " is not in an active trial");
        }

        int updated = promoCodeRepository.incrementUsesCount(code.getId());
        if (updated == 0) {
            // Atomicité : le garde-fou WHERE uses_count < max_uses a coupé l'UPDATE.
            throw new PromoCodeException(PromoCodeErrorCode.PROMO_CODE_EXHAUSTED,
                    "Promo code " + normalized + " is exhausted");
        }

        int valueDays = code.getValueDays();
        Instant newExpiresAt = subscription.getExpiresAt().plus(valueDays, ChronoUnit.DAYS);
        subscription.setExpiresAt(newExpiresAt);
        subscriptionRepository.save(subscription);

        PromoCodeRedemption redemption = new PromoCodeRedemption();
        redemption.setWorkspaceId(workspaceId);
        redemption.setPromoCodeId(code.getId());
        redemption.setCodeAtRedemption(code.getCode());
        redemption.setType(PromoCodeType.TRIAL_EXTENSION);
        redemption.setValueAppliedDays(valueDays);
        redemption.setAppliedByUserId(user.getId());
        redemptionRepository.save(redemption);

        log.info("PromoCode action=REDEEM code={} workspaceId={} userId={} addedDays={} newExpiresAt={}",
                code.getCode(), workspaceId, user.getId(), valueDays, newExpiresAt);

        return new RedeemResponse(newExpiresAt, valueDays, code.getPartnerLabel());
    }

    /**
     * Vérifie l'invariant anti-abus « 1 TRIAL_EXTENSION par workspace à vie ».
     * Méthode applicative qui couvre le test H2 (sans index unique partiel) et
     * la production PostgreSQL (en plus de l'index unique partiel — défense en
     * profondeur).
     */
    void assertWorkspaceCanRedeemTrialExtension(UUID workspaceId) {
        if (redemptionRepository.existsByWorkspaceIdAndType(workspaceId, PromoCodeType.TRIAL_EXTENSION)) {
            throw new PromoCodeException(
                    PromoCodeErrorCode.WORKSPACE_ALREADY_REDEEMED_TRIAL_EXTENSION,
                    "Workspace " + workspaceId
                            + " has already redeemed a TRIAL_EXTENSION promo code");
        }
    }

    private void assertWorkspaceMembership(UUID workspaceId, User user) {
        workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, user.getId())
                .orElseThrow(() -> new PromoCodeException(PromoCodeErrorCode.FORBIDDEN_WORKSPACE,
                        "User is not a member of workspace " + workspaceId));
    }

    private User assertSuperAdmin(OidcUser oidcUser, String provider) {
        try {
            return superAdminService.assertSuperAdmin(oidcUser, provider);
        } catch (org.springframework.web.server.ResponseStatusException e) {
            // Conserver la sémantique applicative : un non-super-admin se voit
            // refusé avec un PromoCodeException porteur du code applicatif, capté
            // par GlobalExceptionHandler. On laisse remonter une éventuelle
            // 404 « User not found » telle quelle (cas anormal).
            if (e.getStatusCode().value() == 403) {
                throw new PromoCodeException(PromoCodeErrorCode.FORBIDDEN_SUPER_ADMIN,
                        "Super-admin access required");
            }
            throw e;
        }
    }

    private void validateTypeValueDays(PromoCodeType type, Integer valueDays) {
        if (type == PromoCodeType.TRIAL_EXTENSION && valueDays == null) {
            throw new PromoCodeException(PromoCodeErrorCode.PROMO_CODE_TYPE_NOT_SUPPORTED_YET,
                    "valueDays is required for TRIAL_EXTENSION");
        }
    }

    private void validateExpiresAtFuture(Instant expiresAt) {
        if (!expiresAt.isAfter(Instant.now())) {
            throw new PromoCodeException(PromoCodeErrorCode.PROMO_CODE_EXPIRED,
                    "expiresAt must be in the future");
        }
    }

    private static String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
