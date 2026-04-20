package fr.ailegalcase.billing;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.UUID;

@Service
public class BillingSeatsService {

    private final PlanLimitService planLimitService;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public BillingSeatsService(PlanLimitService planLimitService,
                               WorkspaceMemberRepository workspaceMemberRepository,
                               CurrentUserResolver currentUserResolver) {
        this.planLimitService = planLimitService;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public SeatsSummaryResponse getSummary(OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        WorkspaceMember requestingMember = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));

        if (!"OWNER".equals(requestingMember.getMemberRole())
                && !"ADMIN".equals(requestingMember.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only OWNER or ADMIN can view seats summary");
        }

        Workspace workspace = requestingMember.getWorkspace();
        UUID workspaceId = workspace.getId();

        String planCode = planLimitService.getPlanCodeForWorkspace(workspaceId);
        int seatCount = Math.max(1, workspaceMemberRepository.findByWorkspace_Id(workspaceId).size());
        int includedSeats = planLimitService.getIncludedSeats(planCode);
        int maxSeats = planLimitService.getMaxSeats(planCode);
        int extraSeatPriceCents = planLimitService.getExtraSeatPriceCents(planCode);
        int baseMonthlyCostCents = planLimitService.getBaseMonthlyCostCents(planCode);
        int extraSeats = Math.max(0, seatCount - includedSeats);
        int totalMonthlyCostCents = baseMonthlyCostCents + (extraSeats * extraSeatPriceCents);

        return new SeatsSummaryResponse(
                planCode, seatCount, includedSeats, maxSeats,
                extraSeatPriceCents, baseMonthlyCostCents, totalMonthlyCostCents);
    }
}
