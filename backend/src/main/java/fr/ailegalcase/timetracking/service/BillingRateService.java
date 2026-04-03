package fr.ailegalcase.timetracking.service;

import fr.ailegalcase.audit.AuditLog;
import fr.ailegalcase.audit.AuditLogRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.timetracking.dto.BillingRateRequest;
import fr.ailegalcase.timetracking.dto.BillingRateResponse;
import fr.ailegalcase.timetracking.entity.UserBillingRate;
import fr.ailegalcase.timetracking.repository.UserBillingRateRepository;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class BillingRateService {

    private final UserBillingRateRepository billingRateRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final AuditLogRepository auditLogRepository;

    public BillingRateService(UserBillingRateRepository billingRateRepository,
                              WorkspaceMemberRepository workspaceMemberRepository,
                              CurrentUserResolver currentUserResolver,
                              AuditLogRepository auditLogRepository) {
        this.billingRateRepository = billingRateRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public BillingRateResponse setRateForMember(UUID targetUserId, BillingRateRequest request,
                                                OidcUser oidcUser, Principal principal) {
        User caller = resolveUser(oidcUser, principal);
        WorkspaceMember callerMember = resolveAdminOrOwner(caller);

        WorkspaceMember targetMember = workspaceMemberRepository
                .findByWorkspace_IdAndUser_Id(callerMember.getWorkspace().getId(), targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Target user does not belong to this workspace"));

        UserBillingRate rate = new UserBillingRate();
        rate.setUser(targetMember.getUser());
        rate.setWorkspace(callerMember.getWorkspace());
        rate.setRatePerHour(request.ratePerHour());
        rate.setEffectiveFrom(LocalDate.now(ZoneOffset.UTC));

        BillingRateResponse saved = BillingRateResponse.from(billingRateRepository.save(rate));

        AuditLog log = new AuditLog();
        log.setWorkspaceId(callerMember.getWorkspace().getId());
        log.setUserId(caller.getId());
        log.setAction("BILLING_RATE_UPDATED");
        log.setMetadata("targetUserId=" + targetUserId + " ratePerHour=" + request.ratePerHour());
        auditLogRepository.save(log);

        return saved;
    }

    @Transactional(readOnly = true)
    public Map<UUID, BillingRateResponse> getMemberRates(OidcUser oidcUser, Principal principal) {
        User caller = resolveUser(oidcUser, principal);
        WorkspaceMember callerMember = resolveAdminOrOwner(caller);
        UUID workspaceId = callerMember.getWorkspace().getId();

        Map<UUID, BillingRateResponse> result = new java.util.LinkedHashMap<>();
        workspaceMemberRepository.findByWorkspace_Id(workspaceId).forEach(m -> {
            UUID userId = m.getUser().getId();
            billingRateRepository.findCurrentRate(userId, workspaceId)
                    .map(BillingRateResponse::from)
                    .ifPresent(rate -> result.put(userId, rate));
        });
        return result;
    }

    @Transactional(readOnly = true)
    public BillingRateResponse getCurrentRate(OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        WorkspaceMember member = resolveWorkspaceMember(user);

        return billingRateRepository
                .findCurrentRate(user.getId(), member.getWorkspace().getId())
                .map(BillingRateResponse::from)
                .orElse(null);
    }

    public Optional<UserBillingRate> getRateAtDate(UUID userId, UUID workspaceId, LocalDate date) {
        return billingRateRepository.findRateAtDate(userId, workspaceId, date);
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private WorkspaceMember resolveWorkspaceMember(User user) {
        return workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
    }

    private WorkspaceMember resolveAdminOrOwner(User user) {
        WorkspaceMember member = resolveWorkspaceMember(user);
        String role = member.getMemberRole();
        if (!"OWNER".equals(role) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only OWNER or ADMIN can set billing rates for members");
        }
        return member;
    }
}
