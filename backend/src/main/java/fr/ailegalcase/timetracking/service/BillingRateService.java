package fr.ailegalcase.timetracking.service;

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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
public class BillingRateService {

    private final UserBillingRateRepository billingRateRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public BillingRateService(UserBillingRateRepository billingRateRepository,
                              WorkspaceMemberRepository workspaceMemberRepository,
                              CurrentUserResolver currentUserResolver) {
        this.billingRateRepository = billingRateRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional
    public BillingRateResponse setRate(BillingRateRequest request, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        WorkspaceMember member = resolveWorkspaceMember(user);

        UserBillingRate rate = new UserBillingRate();
        rate.setUser(user);
        rate.setWorkspace(member.getWorkspace());
        rate.setRatePerHour(request.ratePerHour());
        rate.setEffectiveFrom(LocalDate.now(ZoneOffset.UTC));

        return BillingRateResponse.from(billingRateRepository.save(rate));
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
}
