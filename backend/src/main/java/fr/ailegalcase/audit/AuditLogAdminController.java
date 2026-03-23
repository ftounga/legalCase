package fr.ailegalcase.audit;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditLogAdminController {

    private final AuditLogAdminService auditLogAdminService;

    public AuditLogAdminController(AuditLogAdminService auditLogAdminService) {
        this.auditLogAdminService = auditLogAdminService;
    }

    @GetMapping
    public List<AuditLogResponse> getAuditLogs(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return auditLogAdminService.getAuditLogs(oidcUser, principal);
    }
}
