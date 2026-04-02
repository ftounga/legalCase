package fr.ailegalcase.timetracking;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.timetracking.entity.TimeEntry;
import fr.ailegalcase.timetracking.repository.TimeEntryRepository;
import fr.ailegalcase.timetracking.service.TimeEntryService;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceTest {

    @Mock private TimeEntryRepository timeEntryRepository;
    @Mock private CaseFileRepository caseFileRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private CurrentUserResolver currentUserResolver;

    private TimeEntryService service;

    private User user;
    private Workspace workspace;
    private WorkspaceMember member;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        service = new TimeEntryService(timeEntryRepository, caseFileRepository,
                workspaceMemberRepository, currentUserResolver);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setStatus("ACTIVE");

        workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setName("Test WS");
        workspace.setSlug("test-ws");
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");

        member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);
        member.setMemberRole("MEMBER");

        caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFile.setTitle("Test Dossier");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
    }

    // U-TE-01 : start nominal → entrée créée avec stoppedAt = null
    @Test
    void startTimer_nominal_createsEntryWithNullStoppedAt() {
        when(caseFileRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(caseFile));
        when(timeEntryRepository.existsByUser_IdAndStoppedAtIsNull(user.getId())).thenReturn(false);
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(inv -> {
            TimeEntry e = inv.getArgument(0);
            e.setCaseFile(caseFile);
            e.setUser(user);
            return e;
        });

        var response = service.startTimer(UUID.randomUUID(), null, null);

        assertThat(response.stoppedAt()).isNull();
        assertThat(response.durationSeconds()).isNull();
    }

    // U-TE-02 : start avec timer actif → ConflictException (409)
    @Test
    void startTimer_withActiveTimer_throws409() {
        when(caseFileRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(caseFile));
        when(timeEntryRepository.existsByUser_IdAndStoppedAtIsNull(user.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.startTimer(UUID.randomUUID(), null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    // U-TE-03 : stop nominal → duration_seconds calculé correctement
    @Test
    void stopTimer_nominal_calculatesDuration() {
        TimeEntry entry = new TimeEntry();
        entry.setUser(user);
        entry.setWorkspace(workspace);
        entry.setCaseFile(caseFile);
        entry.setStartedAt(Instant.now().minusSeconds(3600));

        when(timeEntryRepository.findByIdAndWorkspace_Id(any(), any())).thenReturn(Optional.of(entry));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.stopTimer(UUID.randomUUID(), null, null);

        assertThat(response.stoppedAt()).isNotNull();
        assertThat(response.durationSeconds()).isGreaterThan(0);
    }

    // U-TE-04 : stop d'une entrée déjà stoppée → BadRequestException (400)
    @Test
    void stopTimer_alreadyStopped_throws400() {
        TimeEntry entry = new TimeEntry();
        entry.setUser(user);
        entry.setWorkspace(workspace);
        entry.setCaseFile(caseFile);
        entry.setStartedAt(Instant.now().minusSeconds(3600));
        entry.setStoppedAt(Instant.now());
        entry.setDurationSeconds(3600);

        when(timeEntryRepository.findByIdAndWorkspace_Id(any(), any())).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.stopTimer(UUID.randomUUID(), null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // U-TE-05 : stop d'une entrée appartenant à un autre utilisateur → 403
    @Test
    void stopTimer_byOtherUser_throws403() {
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        TimeEntry entry = new TimeEntry();
        entry.setUser(otherUser);
        entry.setWorkspace(workspace);
        entry.setCaseFile(caseFile);
        entry.setStartedAt(Instant.now().minusSeconds(3600));

        when(timeEntryRepository.findByIdAndWorkspace_Id(any(), any())).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.stopTimer(UUID.randomUUID(), null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
