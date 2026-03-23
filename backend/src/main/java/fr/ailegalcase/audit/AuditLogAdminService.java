package fr.ailegalcase.audit;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fr.ailegalcase.shared.OAuthProviderResolver.resolve;

@Service
public class AuditLogAdminService {

    private static final Set<String> ADMIN_ROLES = Set.of("OWNER", "ADMIN");
    private static final Pattern CASE_FILE_TITLE_PATTERN =
            Pattern.compile("\"caseFileTitle\":\"([^\"\\\\]*)\"");
    private static final Pattern DOCUMENT_NAME_PATTERN =
            Pattern.compile("\"documentName\":\"([^\"\\\\]*)\"");

    private final AuditLogRepository auditLogRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserResolver currentUserResolver;

    public AuditLogAdminService(AuditLogRepository auditLogRepository,
                                WorkspaceMemberRepository workspaceMemberRepository,
                                UserRepository userRepository,
                                CurrentUserResolver currentUserResolver) {
        this.auditLogRepository = auditLogRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(OidcUser oidcUser, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, resolve(principal), principal);

        WorkspaceMember member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));

        if (!ADMIN_ROLES.contains(member.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        UUID workspaceId = member.getWorkspace().getId();
        List<AuditLog> logs = auditLogRepository.findTop50ByWorkspaceIdOrderByCreatedAtDesc(workspaceId);

        if (logs.isEmpty()) {
            return List.of();
        }

        Set<UUID> userIds = logs.stream().map(AuditLog::getUserId).collect(Collectors.toSet());
        Map<UUID, String> emailById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));

        return logs.stream()
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getAction(),
                        emailById.getOrDefault(log.getUserId(), ""),
                        log.getCaseFileId(),
                        extractCaseFileTitle(log.getMetadata()),
                        extractDocumentName(log.getMetadata()),
                        log.getCreatedAt()
                ))
                .toList();
    }

    private String extractCaseFileTitle(String metadata) {
        if (metadata == null) return "";
        Matcher m = CASE_FILE_TITLE_PATTERN.matcher(metadata);
        return m.find() ? m.group(1) : "";
    }

    private String extractDocumentName(String metadata) {
        if (metadata == null) return "";
        Matcher m = DOCUMENT_NAME_PATTERN.matcher(metadata);
        return m.find() ? m.group(1) : "";
    }
}
