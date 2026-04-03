package fr.ailegalcase.timetracking;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.timetracking.dto.TimeReportResponse;
import fr.ailegalcase.timetracking.entity.TimeEntry;
import fr.ailegalcase.timetracking.entity.UserBillingRate;
import fr.ailegalcase.timetracking.repository.TimeEntryRepository;
import fr.ailegalcase.timetracking.repository.UserBillingRateRepository;
import fr.ailegalcase.timetracking.service.TimeReportService;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeReportServiceTest {

    @Mock private TimeEntryRepository timeEntryRepository;
    @Mock private UserBillingRateRepository billingRateRepository;
    @Mock private CaseFileRepository caseFileRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private CurrentUserResolver currentUserResolver;

    private TimeReportService service;

    private User user;
    private Workspace workspace;
    private WorkspaceMember ownerMember;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        service = new TimeReportService(timeEntryRepository, billingRateRepository,
                caseFileRepository, workspaceMemberRepository, currentUserResolver);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@example.com");
        user.setStatus("ACTIVE");

        workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        ownerMember = new WorkspaceMember();
        ownerMember.setUser(user);
        ownerMember.setWorkspace(workspace);
        ownerMember.setMemberRole("OWNER");

        caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFile.setTitle("Dossier Test");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
    }

    // U-TR-01 : agrégation mensuelle correcte
    @Test
    void getMonthlyReport_aggregatesHoursAndAmount() {
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(ownerMember));

        TimeEntry entry1 = buildEntry(3600);
        TimeEntry entry2 = buildEntry(1800);
        when(timeEntryRepository.findCompletedByWorkspaceAndMonth(any(), any(), any()))
                .thenReturn(List.of(entry1, entry2));

        UUID caseFileId = caseFile.getId() != null ? caseFile.getId() : UUID.randomUUID();
        when(caseFileRepository.findTitleById(any())).thenReturn(Optional.of("Dossier Test"));

        UserBillingRate rate = new UserBillingRate();
        rate.setRatePerHour(new BigDecimal("200.00"));
        rate.setEffectiveFrom(LocalDate.of(2026, 4, 1));
        when(billingRateRepository.findRateAtDate(any(), any(), any())).thenReturn(Optional.of(rate));

        TimeReportResponse response = service.getMonthlyReport("2026-04", null, null);

        assertThat(response.lines()).hasSize(1);
        assertThat(response.lines().get(0).totalSeconds()).isEqualTo(5400);
        assertThat(response.lines().get(0).amount()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    // U-TR-02 : entrée sans stopped_at exclue du rapport
    @Test
    void getMonthlyReport_openEntryExcluded() {
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(ownerMember));
        when(timeEntryRepository.findCompletedByWorkspaceAndMonth(any(), any(), any()))
                .thenReturn(List.of());

        TimeReportResponse response = service.getMonthlyReport("2026-04", null, null);

        assertThat(response.lines()).isEmpty();
    }

    // U-TR-03 : génération CSV correcte (séparateurs, entêtes, encodage)
    @Test
    void exportCsv_generatesValidCsv() {
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(ownerMember));

        TimeEntry entry = buildEntry(3600);
        when(timeEntryRepository.findCompletedByWorkspaceAndMonth(any(), any(), any()))
                .thenReturn(List.of(entry));
        when(caseFileRepository.findTitleById(any())).thenReturn(Optional.of("Dossier Test"));

        UserBillingRate rate = new UserBillingRate();
        rate.setRatePerHour(new BigDecimal("150.00"));
        rate.setEffectiveFrom(LocalDate.of(2026, 4, 1));
        when(billingRateRepository.findRateAtDate(any(), any(), any())).thenReturn(Optional.of(rate));

        byte[] csv = service.exportCsv("2026-04", null, null);
        String content = new String(csv, StandardCharsets.UTF_8);

        assertThat(content).startsWith("\uFEFF");
        assertThat(content).contains("Dossier,Utilisateur,Heures");
        assertThat(content).contains("Dossier Test");
        assertThat(content).contains("owner@example.com");
    }

    // U-TR-04 : format month invalide → 400 (validation avant résolution user)
    @Test
    void getMonthlyReport_invalidMonth_throws400() {
        assertThatThrownBy(() -> service.getMonthlyReport("invalid", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // U-TR-05 : MEMBER tente d'accéder au rapport → 403
    @Test
    void getMonthlyReport_memberRole_throws403() {
        WorkspaceMember memberRole = new WorkspaceMember();
        memberRole.setUser(user);
        memberRole.setWorkspace(workspace);
        memberRole.setMemberRole("MEMBER");

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(memberRole));

        assertThatThrownBy(() -> service.getMonthlyReport("2026-04", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // U-TR-06 : getMyMonthlyReport accessible à un MEMBER (pas de restriction de rôle)
    @Test
    void getMyMonthlyReport_memberCanAccess() {
        WorkspaceMember memberRole = new WorkspaceMember();
        memberRole.setUser(user);
        memberRole.setWorkspace(workspace);
        memberRole.setMemberRole("MEMBER");

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(memberRole));
        when(timeEntryRepository.findCompletedByWorkspaceUserAndMonth(any(), any(), any(), any()))
                .thenReturn(List.of());

        TimeReportResponse response = service.getMyMonthlyReport("2026-04", null, null);

        assertThat(response).isNotNull();
        assertThat(response.lines()).isEmpty();
    }

    // U-TR-07 : getMyMonthlyReport ne retourne que les entrées de l'utilisateur courant
    @Test
    void getMyMonthlyReport_returnsOnlyCurrentUserEntries() {
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(ownerMember));

        TimeEntry myEntry = buildEntry(3600);
        when(timeEntryRepository.findCompletedByWorkspaceUserAndMonth(
                eq(workspace.getId()), eq(user.getId()), any(), any()))
                .thenReturn(List.of(myEntry));
        when(caseFileRepository.findTitleById(any())).thenReturn(Optional.of("Dossier Test"));
        when(billingRateRepository.findRateAtDate(any(), any(), any())).thenReturn(Optional.empty());

        TimeReportResponse response = service.getMyMonthlyReport("2026-04", null, null);

        assertThat(response.lines()).hasSize(1);
        assertThat(response.lines().get(0).userEmail()).isEqualTo("owner@example.com");
    }

    private TimeEntry buildEntry(int durationSeconds) {
        TimeEntry entry = new TimeEntry();
        entry.setUser(user);
        entry.setWorkspace(workspace);
        entry.setCaseFile(caseFile);
        entry.setStartedAt(Instant.parse("2026-04-10T10:00:00Z"));
        entry.setStoppedAt(Instant.parse("2026-04-10T10:00:00Z").plusSeconds(durationSeconds));
        entry.setDurationSeconds(durationSeconds);
        return entry;
    }
}
