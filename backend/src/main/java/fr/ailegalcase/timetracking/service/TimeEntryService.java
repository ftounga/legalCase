package fr.ailegalcase.timetracking.service;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.timetracking.dto.TimeEntryResponse;
import fr.ailegalcase.timetracking.entity.TimeEntry;
import fr.ailegalcase.timetracking.repository.TimeEntryRepository;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public TimeEntryService(TimeEntryRepository timeEntryRepository,
                            CaseFileRepository caseFileRepository,
                            WorkspaceMemberRepository workspaceMemberRepository,
                            CurrentUserResolver currentUserResolver) {
        this.timeEntryRepository = timeEntryRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional
    public TimeEntryResponse startTimer(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        WorkspaceMember member = resolveWorkspaceMember(user);
        UUID workspaceId = member.getWorkspace().getId();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));

        if (!caseFile.getWorkspace().getId().equals(workspaceId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // SF-106-06 : ignorer les timers de dossiers supprimés (soft delete) et les auto-stop.
        stopOrphanTimers(user.getId());
        if (timeEntryRepository.existsActiveByUserIdAndCaseFileNotDeleted(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A timer is already running. Stop it before starting a new one.");
        }

        TimeEntry entry = new TimeEntry();
        entry.setCaseFile(caseFile);
        entry.setWorkspace(member.getWorkspace());
        entry.setUser(user);
        entry.setStartedAt(Instant.now());

        return TimeEntryResponse.from(timeEntryRepository.save(entry));
    }

    @Transactional
    public TimeEntryResponse stopTimer(UUID entryId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        WorkspaceMember member = resolveWorkspaceMember(user);

        TimeEntry entry = timeEntryRepository.findByIdAndWorkspace_Id(entryId, member.getWorkspace().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time entry not found"));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (entry.getStoppedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timer is already stopped");
        }

        Instant now = Instant.now();
        entry.setStoppedAt(now);
        long durationSeconds = now.getEpochSecond() - entry.getStartedAt().getEpochSecond();
        entry.setDurationSeconds((int) durationSeconds);

        return TimeEntryResponse.from(timeEntryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<TimeEntryResponse> listEntries(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        WorkspaceMember member = resolveWorkspaceMember(user);
        UUID workspaceId = member.getWorkspace().getId();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));

        if (!caseFile.getWorkspace().getId().equals(workspaceId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return timeEntryRepository
                .findByCaseFile_IdAndWorkspace_IdOrderByStartedAtDesc(caseFileId, workspaceId)
                .stream()
                .map(TimeEntryResponse::from)
                .toList();
    }

    /**
     * SF-106-06 : arrête les timers orphelins dont le dossier a été soft-deleted.
     * Appelé avant chaque startTimer pour nettoyer les orphelins silencieusement.
     */
    @Transactional
    public void stopOrphanTimers(UUID userId) {
        List<TimeEntry> all = timeEntryRepository.findActiveByUserId(userId);
        Instant now = Instant.now();
        for (TimeEntry entry : all) {
            if (entry.getCaseFile().getDeletedAt() != null) {
                entry.setStoppedAt(now);
                long dur = now.getEpochSecond() - entry.getStartedAt().getEpochSecond();
                entry.setDurationSeconds((int) Math.min(dur, MAX_AUTO_STOP_SECONDS));
                timeEntryRepository.save(entry);
            }
        }
    }

    /**
     * SF-106-06 : arrête tous les timers actifs d'un utilisateur.
     * Appelé au logout.
     */
    @Transactional
    public void stopAllActive(UUID userId) {
        List<TimeEntry> active = timeEntryRepository.findActiveByUserId(userId);
        Instant now = Instant.now();
        for (TimeEntry entry : active) {
            entry.setStoppedAt(now);
            long dur = now.getEpochSecond() - entry.getStartedAt().getEpochSecond();
            entry.setDurationSeconds((int) Math.min(dur, MAX_AUTO_STOP_SECONDS));
            timeEntryRepository.save(entry);
        }
    }

    private static final int MAX_AUTO_STOP_SECONDS = 8 * 3600;

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
