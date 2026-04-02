package fr.ailegalcase.timetracking.controller;

import fr.ailegalcase.timetracking.dto.TimeEntryResponse;
import fr.ailegalcase.timetracking.service.TimeEntryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    public TimeEntryController(TimeEntryService timeEntryService) {
        this.timeEntryService = timeEntryService;
    }

    @PostMapping("/api/v1/case-files/{caseFileId}/time-entries/start")
    @ResponseStatus(HttpStatus.CREATED)
    public TimeEntryResponse startTimer(@PathVariable UUID caseFileId,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        Principal principal) {
        return timeEntryService.startTimer(caseFileId, oidcUser, principal);
    }

    @PostMapping("/api/v1/time-entries/{id}/stop")
    public TimeEntryResponse stopTimer(@PathVariable UUID id,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return timeEntryService.stopTimer(id, oidcUser, principal);
    }

    @GetMapping("/api/v1/case-files/{caseFileId}/time-entries")
    public List<TimeEntryResponse> listEntries(@PathVariable UUID caseFileId,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return timeEntryService.listEntries(caseFileId, oidcUser, principal);
    }
}
