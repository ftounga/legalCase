package fr.ailegalcase.referential;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.workspace.EmailService;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferentialReportServiceTest {

    @Mock private LegalReferentialRepository referentialRepository;
    @Mock private ReferentialReportRepository reportRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private EmailService emailService;

    private ReferentialReportService service;

    @BeforeEach
    void setUp() {
        service = new ReferentialReportService(referentialRepository, reportRepository,
                workspaceMemberRepository, emailService);
    }

    private LegalReferential entry(UUID id) {
        LegalReferential e = new LegalReferential();
        e.setId(id);
        e.setLabel("Barème Macron — 0 ans");
        e.setEntryKey("0_ANS");
        e.setValueJson("{\"value\":1000}");
        e.setSystem(true);
        e.setActive(true);
        return e;
    }

    private User reporter(UUID id) {
        User u = new User();
        u.setId(id);
        u.setEmail("member@example.com");
        return u;
    }

    private WorkspaceMember ownerMember(String email) {
        User owner = new User();
        owner.setEmail(email);
        Workspace ws = new Workspace();
        WorkspaceMember m = new WorkspaceMember();
        m.setUser(owner);
        m.setWorkspace(ws);
        m.setMemberRole("OWNER");
        return m;
    }

    // RPT-01 : signalement normal → report créé + email envoyé aux OWNER/ADMIN
    @Test
    void report_nominal_createsReportAndSendsEmail() {
        UUID entryId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User reporter = reporter(UUID.randomUUID());

        when(referentialRepository.findById(entryId)).thenReturn(Optional.of(entry(entryId)));
        when(reportRepository.findByEntry_IdAndReporter_IdAndStatus(entryId, reporter.getId(), ReferentialReport.Status.OPEN))
                .thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workspaceMemberRepository.findByWorkspace_IdAndMemberRoleIn(eq(workspaceId), anyList()))
                .thenReturn(List.of(ownerMember("owner@example.com")));

        service.report(entryId, workspaceId, reporter, "La valeur semble incorrecte.");

        verify(reportRepository).save(any(ReferentialReport.class));
        verify(emailService).sendReferentialReport(
                eq("owner@example.com"), anyString(), anyString(),
                eq("La valeur semble incorrecte."), eq("member@example.com"));
    }

    // RPT-02 : doublon → 409
    @Test
    void report_duplicate_throws409() {
        UUID entryId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User reporter = reporter(UUID.randomUUID());

        when(referentialRepository.findById(entryId)).thenReturn(Optional.of(entry(entryId)));
        when(reportRepository.findByEntry_IdAndReporter_IdAndStatus(entryId, reporter.getId(), ReferentialReport.Status.OPEN))
                .thenReturn(Optional.of(new ReferentialReport()));

        assertThatThrownBy(() -> service.report(entryId, workspaceId, reporter, "Doublon"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("déjà signalé");
    }

    // RPT-03 : entrée introuvable → 404
    @Test
    void report_entryNotFound_throws404() {
        UUID entryId = UUID.randomUUID();

        when(referentialRepository.findById(entryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.report(entryId, UUID.randomUUID(), reporter(UUID.randomUUID()), "test"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("introuvable");
    }
}
