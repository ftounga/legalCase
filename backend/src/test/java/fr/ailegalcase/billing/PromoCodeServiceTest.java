package fr.ailegalcase.billing;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.superadmin.SuperAdminService;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromoCodeServiceTest {

    @Mock private PromoCodeRepository promoCodeRepository;
    @Mock private PromoCodeRedemptionRepository redemptionRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private SuperAdminService superAdminService;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private StripePromoCodeService stripePromoCodeService;
    @Mock private OidcUser oidcUser;
    @Mock private Principal principal;

    private PromoCodeService service;

    private User adminUser;
    private User memberUser;

    @BeforeEach
    void setUp() {
        service = new PromoCodeService(promoCodeRepository, redemptionRepository,
                subscriptionRepository, workspaceMemberRepository, superAdminService,
                currentUserResolver, stripePromoCodeService);

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@example.com");
        adminUser.setSuperAdmin(true);

        memberUser = new User();
        memberUser.setId(UUID.randomUUID());
        memberUser.setEmail("member@example.com");
    }

    // ===== Création =====

    @Test
    void createCode_nominal_persistsAndReturnsDto() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);
        when(promoCodeRepository.findByCodeIgnoreCase("ACE2026")).thenReturn(Optional.empty());
        when(promoCodeRepository.save(any())).thenAnswer(inv -> {
            PromoCode c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        PromoCodeCreateRequest req = new PromoCodeCreateRequest(
                "ace2026", PromoCodeType.TRIAL_EXTENSION, 30, "ACE", 100,
                Instant.now().plus(30, ChronoUnit.DAYS));

        PromoCodeDto dto = service.createCode(req, oidcUser, "GOOGLE");

        assertThat(dto.code()).isEqualTo("ACE2026");
        assertThat(dto.type()).isEqualTo(PromoCodeType.TRIAL_EXTENSION);
        assertThat(dto.valueDays()).isEqualTo(30);
        assertThat(dto.partnerLabel()).isEqualTo("ACE");
        assertThat(dto.maxUses()).isEqualTo(100);
        assertThat(dto.usesCount()).isZero();
        assertThat(dto.active()).isTrue();
        assertThat(dto.createdByUserId()).isEqualTo(adminUser.getId());

        ArgumentCaptor<PromoCode> captor = ArgumentCaptor.forClass(PromoCode.class);
        verify(promoCodeRepository).save(captor.capture());
        PromoCode persisted = captor.getValue();
        assertThat(persisted.getCode()).isEqualTo("ACE2026");
        assertThat(persisted.getPartnerLabel()).isEqualTo("ACE");
    }

    @Test
    void createCode_trimsAndUppercasesCode() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);
        when(promoCodeRepository.findByCodeIgnoreCase("ACE2026")).thenReturn(Optional.empty());
        when(promoCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PromoCodeCreateRequest req = new PromoCodeCreateRequest(
                "  ace2026  ", PromoCodeType.TRIAL_EXTENSION, 30, "ACE", 100,
                Instant.now().plus(30, ChronoUnit.DAYS));

        service.createCode(req, oidcUser, "GOOGLE");

        ArgumentCaptor<PromoCode> captor = ArgumentCaptor.forClass(PromoCode.class);
        verify(promoCodeRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("ACE2026");
    }

    @Test
    void createCode_duplicateCode_throwsPromoCodeDuplicate() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);
        PromoCode existing = new PromoCode();
        existing.setCode("ACE2026");
        when(promoCodeRepository.findByCodeIgnoreCase("ACE2026")).thenReturn(Optional.of(existing));

        PromoCodeCreateRequest req = new PromoCodeCreateRequest(
                "ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, "ACE", 100,
                Instant.now().plus(30, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.createCode(req, oidcUser, "GOOGLE"))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.PROMO_CODE_DUPLICATE));
        verify(promoCodeRepository, never()).save(any());
    }

    @Test
    void createCode_nonSuperAdmin_throwsForbiddenSuperAdmin() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Super-admin access required"));

        PromoCodeCreateRequest req = new PromoCodeCreateRequest(
                "ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, "ACE", 100,
                Instant.now().plus(30, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.createCode(req, oidcUser, "GOOGLE"))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.FORBIDDEN_SUPER_ADMIN));
    }

    @Test
    void createCode_trialExtensionMissingValueDays_isRefused() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);

        PromoCodeCreateRequest req = new PromoCodeCreateRequest(
                "ACE2026", PromoCodeType.TRIAL_EXTENSION, null, "ACE", 100,
                Instant.now().plus(30, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.createCode(req, oidcUser, "GOOGLE"))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.PROMO_CODE_TYPE_NOT_SUPPORTED_YET));
    }

    @Test
    void createCode_expiresAtInPast_throwsExpired() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);

        PromoCodeCreateRequest req = new PromoCodeCreateRequest(
                "ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, "ACE", 100,
                Instant.now().minus(1, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.createCode(req, oidcUser, "GOOGLE"))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.PROMO_CODE_EXPIRED));
    }

    @Test
    void createCode_stripeDiscount_canBeStoredWithoutValueDays() throws Exception {
        // SF-255-04 : un STRIPE_DISCOUNT minimal valide demande valueOffType
        // + valueOffAmount + duration. Le test originel SF-01 (qui passait
        // null partout) est mis à jour pour respecter la nouvelle validation.
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);
        when(promoCodeRepository.findByCodeIgnoreCase("DISC2026")).thenReturn(Optional.empty());
        when(promoCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stripePromoCodeService.createCouponAndPromotionCode(any()))
                .thenAnswer(inv -> {
                    PromoCode pc = inv.getArgument(0);
                    pc.setStripeCouponId("coupon_test");
                    return "promo_test";
                });

        PromoCodeCreateRequest req = new PromoCodeCreateRequest(
                "DISC2026", PromoCodeType.STRIPE_DISCOUNT, null,
                PromoCodeValueOffType.PERCENT, 10, null, PromoCodeDuration.ONCE,
                "PARTENAIRE", 50,
                Instant.now().plus(30, ChronoUnit.DAYS));

        PromoCodeDto dto = service.createCode(req, oidcUser, "GOOGLE");
        assertThat(dto.type()).isEqualTo(PromoCodeType.STRIPE_DISCOUNT);
        assertThat(dto.valueDays()).isNull();
        assertThat(dto.valueOffType()).isEqualTo(PromoCodeValueOffType.PERCENT);
        assertThat(dto.valueOffAmount()).isEqualTo(10);
        assertThat(dto.duration()).isEqualTo(PromoCodeDuration.ONCE);
        assertThat(dto.stripeCouponId()).isEqualTo("coupon_test");
        assertThat(dto.stripePromotionCodeId()).isEqualTo("promo_test");
    }

    // ===== Listing =====

    @Test
    void listCodes_recalculatesUsesCountFromRedemptions() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);
        PromoCode c1 = newCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100);
        PromoCode c2 = newCode("OLD2025", PromoCodeType.TRIAL_EXTENSION, 10, 10);
        when(promoCodeRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(c1, c2));
        when(redemptionRepository.countByPromoCodeId(c1.getId())).thenReturn(3L);
        when(redemptionRepository.countByPromoCodeId(c2.getId())).thenReturn(10L);

        List<PromoCodeDto> dtos = service.listCodes(oidcUser, "GOOGLE");

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).code()).isEqualTo("ACE2026");
        assertThat(dtos.get(0).usesCount()).isEqualTo(3L);
        assertThat(dtos.get(1).code()).isEqualTo("OLD2025");
        assertThat(dtos.get(1).usesCount()).isEqualTo(10L);
    }

    @Test
    void listCodes_nonSuperAdmin_throwsForbiddenSuperAdmin() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Super-admin access required"));

        assertThatThrownBy(() -> service.listCodes(oidcUser, "GOOGLE"))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.FORBIDDEN_SUPER_ADMIN));
    }

    // ===== Désactivation =====

    @Test
    void deactivateCode_active_flipsActiveFalse() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);
        PromoCode code = newCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100);
        code.setActive(true);
        when(promoCodeRepository.findById(code.getId())).thenReturn(Optional.of(code));
        when(redemptionRepository.countByPromoCodeId(code.getId())).thenReturn(0L);

        PromoCodeDto dto = service.deactivateCode(code.getId(), oidcUser, "GOOGLE");

        assertThat(dto.active()).isFalse();
        assertThat(code.isActive()).isFalse();
        verify(promoCodeRepository).save(code);
    }

    @Test
    void deactivateCode_alreadyInactive_isIdempotent() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);
        PromoCode code = newCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100);
        code.setActive(false);
        when(promoCodeRepository.findById(code.getId())).thenReturn(Optional.of(code));
        when(redemptionRepository.countByPromoCodeId(code.getId())).thenReturn(0L);

        PromoCodeDto dto = service.deactivateCode(code.getId(), oidcUser, "GOOGLE");

        assertThat(dto.active()).isFalse();
        verify(promoCodeRepository, never()).save(any());
    }

    @Test
    void deactivateCode_unknownId_throwsNotFound() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);
        UUID unknown = UUID.randomUUID();
        when(promoCodeRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateCode(unknown, oidcUser, "GOOGLE"))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.PROMO_CODE_NOT_FOUND));
    }

    // ===== Redemption nominale =====

    @Test
    void redeemTrialExtension_nominal_extendsSubscriptionAndLogsRedemption() {
        UUID workspaceId = UUID.randomUUID();
        when(currentUserResolver.resolve(oidcUser, "GOOGLE", principal)).thenReturn(memberUser);
        setupMember(workspaceId, memberUser, "LAWYER");

        PromoCode code = newCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100);
        when(promoCodeRepository.findByCodeIgnoreCase("ACE2026")).thenReturn(Optional.of(code));
        when(redemptionRepository.existsByWorkspaceIdAndType(workspaceId, PromoCodeType.TRIAL_EXTENSION))
                .thenReturn(false);

        Instant initialExpires = Instant.now().plus(5, ChronoUnit.DAYS);
        Subscription sub = freeSubscription(workspaceId, initialExpires);
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));
        when(promoCodeRepository.incrementUsesCount(code.getId())).thenReturn(1);

        RedeemRequest req = new RedeemRequest("ace2026");
        RedeemResponse resp = service.redeemTrialExtension(workspaceId, req, oidcUser, "GOOGLE", principal);

        assertThat(resp.addedDays()).isEqualTo(30);
        assertThat(resp.partnerLabel()).isEqualTo("ACE");
        Instant expected = initialExpires.plus(30, ChronoUnit.DAYS);
        assertThat(resp.newExpiresAt()).isEqualTo(expected);
        assertThat(sub.getExpiresAt()).isEqualTo(expected);
        verify(subscriptionRepository).save(sub);

        ArgumentCaptor<PromoCodeRedemption> rcap = ArgumentCaptor.forClass(PromoCodeRedemption.class);
        verify(redemptionRepository).save(rcap.capture());
        PromoCodeRedemption persisted = rcap.getValue();
        assertThat(persisted.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(persisted.getPromoCodeId()).isEqualTo(code.getId());
        assertThat(persisted.getCodeAtRedemption()).isEqualTo("ACE2026");
        assertThat(persisted.getType()).isEqualTo(PromoCodeType.TRIAL_EXTENSION);
        assertThat(persisted.getValueAppliedDays()).isEqualTo(30);
        assertThat(persisted.getAppliedByUserId()).isEqualTo(memberUser.getId());
    }

    // ===== Redemption — erreurs =====

    @Test
    void redeemTrialExtension_userNotMember_throwsForbiddenWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        when(currentUserResolver.resolve(oidcUser, "GOOGLE", principal)).thenReturn(memberUser);
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, memberUser.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeemTrialExtension(workspaceId,
                new RedeemRequest("ACE2026"), oidcUser, "GOOGLE", principal))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.FORBIDDEN_WORKSPACE));
        verifyNoInteractions(promoCodeRepository);
    }

    @Test
    void redeemTrialExtension_codeNotFound_throwsNotFound() {
        UUID workspaceId = UUID.randomUUID();
        when(currentUserResolver.resolve(oidcUser, "GOOGLE", principal)).thenReturn(memberUser);
        setupMember(workspaceId, memberUser, "OWNER");
        when(promoCodeRepository.findByCodeIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeemTrialExtension(workspaceId,
                new RedeemRequest("unknown"), oidcUser, "GOOGLE", principal))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.PROMO_CODE_NOT_FOUND));
    }

    @Test
    void redeemTrialExtension_stripeDiscountType_throwsNotSupportedYet() {
        UUID workspaceId = UUID.randomUUID();
        when(currentUserResolver.resolve(oidcUser, "GOOGLE", principal)).thenReturn(memberUser);
        setupMember(workspaceId, memberUser, "OWNER");
        PromoCode code = newCode("DISC2026", PromoCodeType.STRIPE_DISCOUNT, null, 100);
        when(promoCodeRepository.findByCodeIgnoreCase("DISC2026")).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.redeemTrialExtension(workspaceId,
                new RedeemRequest("DISC2026"), oidcUser, "GOOGLE", principal))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.PROMO_CODE_TYPE_NOT_SUPPORTED_YET));
    }

    @Test
    void redeemTrialExtension_codeInactive_throwsInactive() {
        UUID workspaceId = UUID.randomUUID();
        when(currentUserResolver.resolve(oidcUser, "GOOGLE", principal)).thenReturn(memberUser);
        setupMember(workspaceId, memberUser, "OWNER");
        PromoCode code = newCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100);
        code.setActive(false);
        when(promoCodeRepository.findByCodeIgnoreCase("ACE2026")).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.redeemTrialExtension(workspaceId,
                new RedeemRequest("ACE2026"), oidcUser, "GOOGLE", principal))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.PROMO_CODE_INACTIVE));
    }

    @Test
    void redeemTrialExtension_codeExpired_throwsExpired() {
        UUID workspaceId = UUID.randomUUID();
        when(currentUserResolver.resolve(oidcUser, "GOOGLE", principal)).thenReturn(memberUser);
        setupMember(workspaceId, memberUser, "OWNER");
        PromoCode code = newCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100);
        code.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(promoCodeRepository.findByCodeIgnoreCase("ACE2026")).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.redeemTrialExtension(workspaceId,
                new RedeemRequest("ACE2026"), oidcUser, "GOOGLE", principal))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.PROMO_CODE_EXPIRED));
    }

    @Test
    void redeemTrialExtension_workspaceAlreadyRedeemed_throwsAlreadyRedeemed() {
        UUID workspaceId = UUID.randomUUID();
        when(currentUserResolver.resolve(oidcUser, "GOOGLE", principal)).thenReturn(memberUser);
        setupMember(workspaceId, memberUser, "OWNER");
        PromoCode code = newCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100);
        when(promoCodeRepository.findByCodeIgnoreCase("ACE2026")).thenReturn(Optional.of(code));
        when(redemptionRepository.existsByWorkspaceIdAndType(workspaceId, PromoCodeType.TRIAL_EXTENSION))
                .thenReturn(true);

        assertThatThrownBy(() -> service.redeemTrialExtension(workspaceId,
                new RedeemRequest("ACE2026"), oidcUser, "GOOGLE", principal))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.WORKSPACE_ALREADY_REDEEMED_TRIAL_EXTENSION));
        verify(promoCodeRepository, never()).incrementUsesCount(any());
    }

    @Test
    void redeemTrialExtension_workspacePaidPlan_throwsNotTrialing() {
        UUID workspaceId = UUID.randomUUID();
        when(currentUserResolver.resolve(oidcUser, "GOOGLE", principal)).thenReturn(memberUser);
        setupMember(workspaceId, memberUser, "OWNER");
        PromoCode code = newCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100);
        when(promoCodeRepository.findByCodeIgnoreCase("ACE2026")).thenReturn(Optional.of(code));
        when(redemptionRepository.existsByWorkspaceIdAndType(workspaceId, PromoCodeType.TRIAL_EXTENSION))
                .thenReturn(false);

        Subscription sub = new Subscription();
        sub.setWorkspaceId(workspaceId);
        sub.setPlanCode("SOLO");
        sub.setStatus("ACTIVE");
        sub.setExpiresAt(null);
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.redeemTrialExtension(workspaceId,
                new RedeemRequest("ACE2026"), oidcUser, "GOOGLE", principal))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.WORKSPACE_NOT_TRIALING));
        verify(promoCodeRepository, never()).incrementUsesCount(any());
    }

    @Test
    void redeemTrialExtension_workspaceTrialExpired_throwsNotTrialing() {
        UUID workspaceId = UUID.randomUUID();
        when(currentUserResolver.resolve(oidcUser, "GOOGLE", principal)).thenReturn(memberUser);
        setupMember(workspaceId, memberUser, "OWNER");
        PromoCode code = newCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100);
        when(promoCodeRepository.findByCodeIgnoreCase("ACE2026")).thenReturn(Optional.of(code));
        when(redemptionRepository.existsByWorkspaceIdAndType(workspaceId, PromoCodeType.TRIAL_EXTENSION))
                .thenReturn(false);

        Subscription sub = freeSubscription(workspaceId, Instant.now().minus(1, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.redeemTrialExtension(workspaceId,
                new RedeemRequest("ACE2026"), oidcUser, "GOOGLE", principal))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.WORKSPACE_NOT_TRIALING));
    }

    @Test
    void redeemTrialExtension_codeExhausted_throwsExhausted() {
        UUID workspaceId = UUID.randomUUID();
        when(currentUserResolver.resolve(oidcUser, "GOOGLE", principal)).thenReturn(memberUser);
        setupMember(workspaceId, memberUser, "OWNER");
        PromoCode code = newCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 1);
        when(promoCodeRepository.findByCodeIgnoreCase("ACE2026")).thenReturn(Optional.of(code));
        when(redemptionRepository.existsByWorkspaceIdAndType(workspaceId, PromoCodeType.TRIAL_EXTENSION))
                .thenReturn(false);
        Subscription sub = freeSubscription(workspaceId, Instant.now().plus(5, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));
        // Garde-fou atomique : UPDATE retourne 0 rows car uses_count >= max_uses.
        when(promoCodeRepository.incrementUsesCount(code.getId())).thenReturn(0);

        assertThatThrownBy(() -> service.redeemTrialExtension(workspaceId,
                new RedeemRequest("ACE2026"), oidcUser, "GOOGLE", principal))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.PROMO_CODE_EXHAUSTED));
        verify(subscriptionRepository, never()).save(any());
        verify(redemptionRepository, never()).save(any());
    }

    @Test
    void redeemTrialExtension_trialExtensionWithoutValueDays_isRefused() {
        UUID workspaceId = UUID.randomUUID();
        when(currentUserResolver.resolve(oidcUser, "GOOGLE", principal)).thenReturn(memberUser);
        setupMember(workspaceId, memberUser, "OWNER");
        PromoCode code = newCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, null, 100);
        when(promoCodeRepository.findByCodeIgnoreCase("ACE2026")).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.redeemTrialExtension(workspaceId,
                new RedeemRequest("ACE2026"), oidcUser, "GOOGLE", principal))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.PROMO_CODE_TYPE_NOT_SUPPORTED_YET));
    }

    @Test
    void redeemTrialExtension_noSubscription_throwsNotTrialing() {
        UUID workspaceId = UUID.randomUUID();
        when(currentUserResolver.resolve(oidcUser, "GOOGLE", principal)).thenReturn(memberUser);
        setupMember(workspaceId, memberUser, "OWNER");
        PromoCode code = newCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100);
        when(promoCodeRepository.findByCodeIgnoreCase("ACE2026")).thenReturn(Optional.of(code));
        when(redemptionRepository.existsByWorkspaceIdAndType(workspaceId, PromoCodeType.TRIAL_EXTENSION))
                .thenReturn(false);
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeemTrialExtension(workspaceId,
                new RedeemRequest("ACE2026"), oidcUser, "GOOGLE", principal))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.WORKSPACE_NOT_TRIALING));
    }

    // ===== Isolation workspace =====

    @Test
    void redeemTrialExtension_isolation_doesNotTouchOtherWorkspaceSubscription() {
        UUID currentWorkspaceId = UUID.randomUUID();
        UUID otherWorkspaceId = UUID.randomUUID();
        when(currentUserResolver.resolve(oidcUser, "GOOGLE", principal)).thenReturn(memberUser);
        setupMember(currentWorkspaceId, memberUser, "OWNER");
        PromoCode code = newCode("ACE2026", PromoCodeType.TRIAL_EXTENSION, 30, 100);
        when(promoCodeRepository.findByCodeIgnoreCase("ACE2026")).thenReturn(Optional.of(code));
        when(redemptionRepository.existsByWorkspaceIdAndType(currentWorkspaceId, PromoCodeType.TRIAL_EXTENSION))
                .thenReturn(false);
        Subscription sub = freeSubscription(currentWorkspaceId, Instant.now().plus(5, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(currentWorkspaceId)).thenReturn(Optional.of(sub));
        when(promoCodeRepository.incrementUsesCount(code.getId())).thenReturn(1);

        service.redeemTrialExtension(currentWorkspaceId,
                new RedeemRequest("ACE2026"), oidcUser, "GOOGLE", principal);

        verify(subscriptionRepository).findByWorkspaceId(currentWorkspaceId);
        verify(subscriptionRepository, never()).findByWorkspaceId(eq(otherWorkspaceId));
    }

    // ===== SF-255-04 — STRIPE_DISCOUNT =====

    @Test
    void createCode_stripeDiscount_nominalPercent_callsStripeAndPersists() throws Exception {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);
        when(promoCodeRepository.findByCodeIgnoreCase("DISC20")).thenReturn(Optional.empty());
        when(promoCodeRepository.save(any())).thenAnswer(inv -> {
            PromoCode c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(stripePromoCodeService.createCouponAndPromotionCode(any())).thenAnswer(inv -> {
            PromoCode pc = inv.getArgument(0);
            pc.setStripeCouponId("coupon_xx");
            return "promo_yy";
        });

        PromoCodeCreateRequest req = new PromoCodeCreateRequest(
                "DISC20", PromoCodeType.STRIPE_DISCOUNT, null,
                PromoCodeValueOffType.PERCENT, 20, null, PromoCodeDuration.REPEATING_3,
                "ACE", 50, Instant.now().plus(60, ChronoUnit.DAYS));

        PromoCodeDto dto = service.createCode(req, oidcUser, "GOOGLE");

        assertThat(dto.type()).isEqualTo(PromoCodeType.STRIPE_DISCOUNT);
        assertThat(dto.valueOffType()).isEqualTo(PromoCodeValueOffType.PERCENT);
        assertThat(dto.valueOffAmount()).isEqualTo(20);
        assertThat(dto.duration()).isEqualTo(PromoCodeDuration.REPEATING_3);
        assertThat(dto.stripeCouponId()).isEqualTo("coupon_xx");
        assertThat(dto.stripePromotionCodeId()).isEqualTo("promo_yy");

        verify(stripePromoCodeService).createCouponAndPromotionCode(any());
        ArgumentCaptor<PromoCode> cap = ArgumentCaptor.forClass(PromoCode.class);
        verify(promoCodeRepository).save(cap.capture());
        assertThat(cap.getValue().getStripePromotionCodeId()).isEqualTo("promo_yy");
    }

    @Test
    void createCode_stripeDiscount_withValueDays_isRefused() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);

        PromoCodeCreateRequest req = new PromoCodeCreateRequest(
                "MIX", PromoCodeType.STRIPE_DISCOUNT, 30,
                PromoCodeValueOffType.PERCENT, 10, null, PromoCodeDuration.ONCE,
                "ACE", 50, Instant.now().plus(30, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.createCode(req, oidcUser, "GOOGLE"))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.PROMO_CODE_TYPE_NOT_SUPPORTED_YET));
        verifyNoInteractions(stripePromoCodeService);
        verify(promoCodeRepository, never()).save(any());
    }

    @Test
    void createCode_stripeDiscount_missingValueOffType_isRefused() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);

        PromoCodeCreateRequest req = new PromoCodeCreateRequest(
                "MISS", PromoCodeType.STRIPE_DISCOUNT, null,
                null, 10, null, PromoCodeDuration.ONCE,
                "ACE", 50, Instant.now().plus(30, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.createCode(req, oidcUser, "GOOGLE"))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.PROMO_CODE_TYPE_NOT_SUPPORTED_YET));
        verifyNoInteractions(stripePromoCodeService);
    }

    @Test
    void createCode_stripeDiscount_percentOutOfRange_isRefused() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);

        PromoCodeCreateRequest req = new PromoCodeCreateRequest(
                "BAD", PromoCodeType.STRIPE_DISCOUNT, null,
                PromoCodeValueOffType.PERCENT, 150, null, PromoCodeDuration.ONCE,
                "ACE", 50, Instant.now().plus(30, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.createCode(req, oidcUser, "GOOGLE"))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.PROMO_CODE_TYPE_NOT_SUPPORTED_YET));
        verifyNoInteractions(stripePromoCodeService);
    }

    @Test
    void createCode_stripeDiscount_stripeFails_throws502AndNoInsert() throws Exception {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);
        when(promoCodeRepository.findByCodeIgnoreCase("DOWN")).thenReturn(Optional.empty());
        when(stripePromoCodeService.createCouponAndPromotionCode(any()))
                .thenThrow(new com.stripe.exception.ApiException("down", "req_x", null, 500, null));

        PromoCodeCreateRequest req = new PromoCodeCreateRequest(
                "DOWN", PromoCodeType.STRIPE_DISCOUNT, null,
                PromoCodeValueOffType.PERCENT, 10, null, PromoCodeDuration.ONCE,
                "ACE", 50, Instant.now().plus(30, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.createCode(req, oidcUser, "GOOGLE"))
                .isInstanceOf(PromoCodeException.class)
                .satisfies(e -> assertThat(((PromoCodeException) e).getCode())
                        .isEqualTo(PromoCodeErrorCode.STRIPE_API_UNAVAILABLE));
        verify(promoCodeRepository, never()).save(any());
    }

    @Test
    void deactivateCode_stripeDiscount_propagatesToStripe() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);
        PromoCode code = newCode("DISC", PromoCodeType.STRIPE_DISCOUNT, null, 50);
        code.setStripePromotionCodeId("promo_to_kill");
        code.setActive(true);
        when(promoCodeRepository.findById(code.getId())).thenReturn(Optional.of(code));
        when(redemptionRepository.countByPromoCodeId(code.getId())).thenReturn(0L);

        service.deactivateCode(code.getId(), oidcUser, "GOOGLE");

        verify(stripePromoCodeService).deactivatePromotionCode("promo_to_kill");
        assertThat(code.isActive()).isFalse();
    }

    @Test
    void deactivateCode_trialExtension_noStripeCall() {
        when(superAdminService.assertSuperAdmin(oidcUser, "GOOGLE")).thenReturn(adminUser);
        PromoCode code = newCode("TRIAL", PromoCodeType.TRIAL_EXTENSION, 30, 50);
        code.setActive(true);
        when(promoCodeRepository.findById(code.getId())).thenReturn(Optional.of(code));
        when(redemptionRepository.countByPromoCodeId(code.getId())).thenReturn(0L);

        service.deactivateCode(code.getId(), oidcUser, "GOOGLE");

        verify(stripePromoCodeService, never()).deactivatePromotionCode(any());
    }

    // ===== Helpers =====

    private PromoCode newCode(String value, PromoCodeType type, Integer valueDays, int maxUses) {
        PromoCode c = new PromoCode();
        c.setId(UUID.randomUUID());
        c.setCode(value);
        c.setType(type);
        c.setValueDays(valueDays);
        c.setPartnerLabel("ACE");
        c.setMaxUses(maxUses);
        c.setUsesCount(0);
        c.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        c.setActive(true);
        c.setCreatedAt(Instant.now());
        c.setCreatedByUserId(UUID.randomUUID());
        return c;
    }

    private Subscription freeSubscription(UUID workspaceId, Instant expiresAt) {
        Subscription sub = new Subscription();
        sub.setWorkspaceId(workspaceId);
        sub.setPlanCode("FREE");
        sub.setStatus("ACTIVE");
        sub.setStartedAt(Instant.now().minus(7, ChronoUnit.DAYS));
        sub.setExpiresAt(expiresAt);
        return sub;
    }

    private void setupMember(UUID workspaceId, User user, String role) {
        Workspace ws = new Workspace();
        ws.setId(workspaceId);
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);
        member.setUser(user);
        member.setMemberRole(role);
        lenient().when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, user.getId()))
                .thenReturn(Optional.of(member));
    }
}
