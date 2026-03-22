package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static fr.ailegalcase.shared.OAuthProviderResolver.resolve;

@Service
public class AdminUsageService {

    private static final Set<String> ADMIN_ROLES = Set.of("OWNER", "ADMIN");

    private final AuthAccountRepository authAccountRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CaseFileRepository caseFileRepository;
    private final UsageEventRepository usageEventRepository;
    private final UserRepository userRepository;
    private final PlanLimitService planLimitService;

    public AdminUsageService(AuthAccountRepository authAccountRepository,
                             WorkspaceMemberRepository workspaceMemberRepository,
                             CaseFileRepository caseFileRepository,
                             UsageEventRepository usageEventRepository,
                             UserRepository userRepository,
                             PlanLimitService planLimitService) {
        this.authAccountRepository = authAccountRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.caseFileRepository = caseFileRepository;
        this.usageEventRepository = usageEventRepository;
        this.userRepository = userRepository;
        this.planLimitService = planLimitService;
    }

    @Transactional(readOnly = true)
    public WorkspaceUsageSummaryResponse getWorkspaceSummary(OidcUser oidcUser, Principal principal) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(resolve(principal), oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();

        WorkspaceMember member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));

        if (!ADMIN_ROLES.contains(member.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Workspace workspace = member.getWorkspace();
        UUID workspaceId = workspace.getId();

        Instant startOfMonth = Instant.now()
                .atOffset(ZoneOffset.UTC)
                .with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0).withMinute(0).withSecond(0).withNano(0)
                .toInstant();
        long monthlyTokensUsed = usageEventRepository.sumTokensByWorkspaceIdSince(workspaceId, startOfMonth);
        long monthlyTokensBudget = planLimitService.getMonthlyTokenBudgetForWorkspace(workspaceId);

        List<CaseFile> caseFiles = caseFileRepository.findByWorkspace_Id(workspaceId);
        if (caseFiles.isEmpty()) {
            return new WorkspaceUsageSummaryResponse(0, 0, BigDecimal.ZERO, List.of(), List.of(),
                    monthlyTokensUsed, monthlyTokensBudget);
        }

        Set<UUID> caseFileIds = caseFiles.stream().map(CaseFile::getId).collect(Collectors.toSet());
        Map<UUID, String> titleById = caseFiles.stream()
                .collect(Collectors.toMap(CaseFile::getId, CaseFile::getTitle));

        List<UsageEvent> events = usageEventRepository.findByCaseFileIdIn(caseFileIds);
        if (events.isEmpty()) {
            return new WorkspaceUsageSummaryResponse(0, 0, BigDecimal.ZERO, List.of(), List.of(),
                    monthlyTokensUsed, monthlyTokensBudget);
        }

        // Totaux globaux
        int totalInput = events.stream().mapToInt(UsageEvent::getTokensInput).sum();
        int totalOutput = events.stream().mapToInt(UsageEvent::getTokensOutput).sum();
        BigDecimal totalCost = events.stream()
                .map(UsageEvent::getEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Agrégation par dossier
        Map<UUID, List<UsageEvent>> byCaseFileId = events.stream()
                .collect(Collectors.groupingBy(UsageEvent::getCaseFileId));
        List<CaseFileUsageSummary> byCaseFile = byCaseFileId.entrySet().stream()
                .map(e -> new CaseFileUsageSummary(
                        e.getKey(),
                        titleById.getOrDefault(e.getKey(), ""),
                        e.getValue().stream().mapToInt(UsageEvent::getTokensInput).sum(),
                        e.getValue().stream().mapToInt(UsageEvent::getTokensOutput).sum(),
                        e.getValue().stream().map(UsageEvent::getEstimatedCost).reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .toList();

        // Agrégation par utilisateur
        Set<UUID> userIds = events.stream().map(UsageEvent::getUserId).collect(Collectors.toSet());
        Map<UUID, String> emailById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));

        Map<UUID, List<UsageEvent>> byUserId = events.stream()
                .collect(Collectors.groupingBy(UsageEvent::getUserId));
        List<UserUsageSummary> byUser = byUserId.entrySet().stream()
                .map(e -> new UserUsageSummary(
                        e.getKey(),
                        emailById.getOrDefault(e.getKey(), ""),
                        e.getValue().stream().mapToInt(UsageEvent::getTokensInput).sum(),
                        e.getValue().stream().mapToInt(UsageEvent::getTokensOutput).sum(),
                        e.getValue().stream().map(UsageEvent::getEstimatedCost).reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .toList();

        return new WorkspaceUsageSummaryResponse(totalInput, totalOutput, totalCost, byUser, byCaseFile,
                monthlyTokensUsed, monthlyTokensBudget);
    }
}
