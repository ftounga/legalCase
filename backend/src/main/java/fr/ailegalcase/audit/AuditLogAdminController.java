package fr.ailegalcase.audit;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
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
            Principal principal,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return auditLogAdminService.getAuditLogs(oidcUser, principal, from, to);
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        byte[] csv = auditLogAdminService.exportCsv(oidcUser, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-log.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
