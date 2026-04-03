package fr.ailegalcase.timetracking.controller;

import fr.ailegalcase.timetracking.dto.TimeReportResponse;
import fr.ailegalcase.timetracking.service.TimeReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/workspace/time-report")
public class TimeReportController {

    private final TimeReportService timeReportService;

    public TimeReportController(TimeReportService timeReportService) {
        this.timeReportService = timeReportService;
    }

    @GetMapping
    public TimeReportResponse getMonthlyReport(@RequestParam String month,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return timeReportService.getMonthlyReport(month, oidcUser, principal);
    }

    @GetMapping("/my")
    public TimeReportResponse getMyMonthlyReport(@RequestParam String month,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return timeReportService.getMyMonthlyReport(month, oidcUser, principal);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(@RequestParam String month,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        byte[] csv = timeReportService.exportCsv(month, oidcUser, principal);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"rapport-temps-" + month + ".csv\"")
                .body(csv);
    }
}
