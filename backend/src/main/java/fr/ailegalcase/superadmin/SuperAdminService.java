package fr.ailegalcase.superadmin;

import fr.ailegalcase.analysis.UsageEventRepository;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SuperAdminService {

    private final AuthAccountRepository authAccountRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UsageEventRepository usageEventRepository;

    public SuperAdminService(AuthAccountRepository authAccountRepository,
                             WorkspaceRepository workspaceRepository,
                             WorkspaceMemberRepository workspaceMemberRepository,
                             SubscriptionRepository subscriptionRepository,
                             UsageEventRepository usageEventRepository) {
        this.authAccountRepository = authAccountRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.usageEventRepository = usageEventRepository;
    }

    @Transactional(readOnly = true)
    public List<SuperAdminWorkspaceResponse> listAllWorkspaces(OidcUser oidcUser, String provider) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();

        if (!user.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super-admin access required");
        }

        return workspaceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SuperAdminUsageResponse> getUsageByWorkspace(OidcUser oidcUser, String provider) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();

        if (!user.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super-admin access required");
        }

        record UsageRow(long tokensInput, long tokensOutput, BigDecimal cost) {}

        Map<UUID, UsageRow> usageByWorkspace = usageEventRepository.aggregateByWorkspaceId().stream()
                .collect(Collectors.toMap(
                        row -> toUUID(row[0]),
                        row -> new UsageRow(
                                ((Number) row[1]).longValue(),
                                ((Number) row[2]).longValue(),
                                new BigDecimal(row[3].toString())
                        )
                ));

        return workspaceRepository.findAll().stream()
                .map(ws -> {
                    UsageRow row = usageByWorkspace.getOrDefault(ws.getId(),
                            new UsageRow(0, 0, BigDecimal.ZERO));
                    return new SuperAdminUsageResponse(ws.getId(), ws.getName(),
                            row.tokensInput(), row.tokensOutput(), row.cost());
                })
                .toList();
    }

    private static UUID toUUID(Object obj) {
        if (obj instanceof UUID u) return u;
        if (obj instanceof byte[] bytes) {
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            return new UUID(bb.getLong(), bb.getLong());
        }
        return UUID.fromString(obj.toString());
    }

    private SuperAdminWorkspaceResponse toResponse(Workspace ws) {
        Instant expiresAt = subscriptionRepository.findByWorkspaceId(ws.getId())
                .map(Subscription::getExpiresAt)
                .orElse(null);
        long memberCount = workspaceMemberRepository.findByWorkspace_Id(ws.getId()).size();
        return new SuperAdminWorkspaceResponse(
                ws.getId(), ws.getName(), ws.getSlug(),
                ws.getPlanCode(), ws.getStatus(), expiresAt,
                memberCount, ws.getCreatedAt()
        );
    }
}
