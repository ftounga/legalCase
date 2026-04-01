package fr.ailegalcase.casefile;

import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CaseDeadlineServiceTest {

    private final CaseDeadlineRepository deadlineRepository = mock(CaseDeadlineRepository.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

    private final CaseDeadlineService service = new CaseDeadlineService(
            deadlineRepository, caseFileRepository, workspaceMemberRepository, currentUserResolver);

    // --- helpers ---

    private CaseAnalysis buildAnalysis() {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        CaseFile caseFile = new CaseFile();
        caseFile.setId(UUID.randomUUID());
        caseFile.setWorkspace(workspace);

        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setId(UUID.randomUUID());
        analysis.setCaseFile(caseFile);
        return analysis;
    }

    private CaseDeadline buildDeadline(String aiStatus, UUID caseFileId) {
        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);
        CaseDeadline d = new CaseDeadline();
        d.setId(UUID.randomUUID());
        d.setCaseFile(caseFile);
        d.setLabel("Délai");
        d.setDueDate(LocalDate.of(2026, 6, 1));
        d.setSource("AI");
        d.setAiStatus(aiStatus);
        return d;
    }

    // ===== createAiDetectedDeadlines =====

    @Test
    void createAiDetectedDeadlines_twoValidEntries_persistsTwoDeadlines() {
        CaseAnalysis analysis = buildAnalysis();
        when(deadlineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String json = """
                {"delais_detectes": [
                    {"label": "Délai de recours", "date_detectee": "2026-06-01", "source": "Document 0"},
                    {"label": "Délai de prescription", "date_detectee": "2026-12-31", "source": "Document 1"}
                ]}
                """;

        service.createAiDetectedDeadlines(analysis, json);

        ArgumentCaptor<CaseDeadline> captor = ArgumentCaptor.forClass(CaseDeadline.class);
        verify(deadlineRepository, times(2)).save(captor.capture());
        List<CaseDeadline> saved = captor.getAllValues();

        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getLabel()).isEqualTo("Délai de recours");
        assertThat(saved.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(saved.get(0).getSource()).isEqualTo("AI");
        assertThat(saved.get(0).getAiStatus()).isEqualTo("PENDING");
        assertThat(saved.get(1).getLabel()).isEqualTo("Délai de prescription");
    }

    @Test
    void createAiDetectedDeadlines_invalidDate_entryIgnoredNoException() {
        CaseAnalysis analysis = buildAnalysis();
        when(deadlineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String json = """
                {"delais_detectes": [
                    {"label": "Délai valide", "date_detectee": "2026-06-01", "source": "Document 0"},
                    {"label": "Délai invalide", "date_detectee": "not-a-date", "source": "Document 1"}
                ]}
                """;

        service.createAiDetectedDeadlines(analysis, json);

        // Only the valid one should be saved
        verify(deadlineRepository, times(1)).save(any());
    }

    @Test
    void createAiDetectedDeadlines_fieldAbsent_noDeadlineCreated() {
        CaseAnalysis analysis = buildAnalysis();

        String json = """
                {"faits": ["fait1"], "risques": []}
                """;

        service.createAiDetectedDeadlines(analysis, json);

        verify(deadlineRepository, never()).save(any());
    }

    @Test
    void createAiDetectedDeadlines_emptyArray_noDeadlineCreated() {
        CaseAnalysis analysis = buildAnalysis();

        service.createAiDetectedDeadlines(analysis, "{\"delais_detectes\": []}");

        verify(deadlineRepository, never()).save(any());
    }

    @Test
    void createAiDetectedDeadlines_malformedJson_noExceptionThrown() {
        CaseAnalysis analysis = buildAnalysis();

        // Should not throw, fail-open
        service.createAiDetectedDeadlines(analysis, "{ not valid json }");

        verify(deadlineRepository, never()).save(any());
    }

    @Test
    void createAiDetectedDeadlines_nullJson_noDeadlineCreated() {
        CaseAnalysis analysis = buildAnalysis();

        service.createAiDetectedDeadlines(analysis, null);

        verify(deadlineRepository, never()).save(any());
    }

    // ===== validate =====

    private void setupValidateFixtures(UUID caseFileId, UUID deadlineId, CaseDeadline deadline) {
        User user = new User();
        user.setId(UUID.randomUUID());

        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);
        caseFile.setWorkspace(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
        when(deadlineRepository.findById(deadlineId)).thenReturn(Optional.of(deadline));
        when(deadlineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void validate_accept_setsAiStatusAccepted() {
        UUID caseFileId = UUID.randomUUID();
        UUID deadlineId = UUID.randomUUID();
        CaseDeadline deadline = buildDeadline("PENDING", caseFileId);
        deadline.setId(deadlineId);
        setupValidateFixtures(caseFileId, deadlineId, deadline);

        CaseDeadlineResponse resp = service.validate(caseFileId, deadlineId, "ACCEPT", null, null);

        assertThat(resp).isNotNull();
        assertThat(resp.aiStatus()).isEqualTo("ACCEPTED");
        verify(deadlineRepository).save(deadline);
        verify(deadlineRepository, never()).delete(any());
    }

    @Test
    void validate_reject_deletesDeadline() {
        UUID caseFileId = UUID.randomUUID();
        UUID deadlineId = UUID.randomUUID();
        CaseDeadline deadline = buildDeadline("PENDING", caseFileId);
        deadline.setId(deadlineId);
        setupValidateFixtures(caseFileId, deadlineId, deadline);

        CaseDeadlineResponse resp = service.validate(caseFileId, deadlineId, "REJECT", null, null);

        assertThat(resp).isNull();
        verify(deadlineRepository).delete(deadline);
        verify(deadlineRepository, never()).save(any());
    }

    @Test
    void validate_alreadyAccepted_throws409() {
        UUID caseFileId = UUID.randomUUID();
        UUID deadlineId = UUID.randomUUID();
        CaseDeadline deadline = buildDeadline("ACCEPTED", caseFileId);
        deadline.setId(deadlineId);
        setupValidateFixtures(caseFileId, deadlineId, deadline);

        assertThatThrownBy(() ->
                service.validate(caseFileId, deadlineId, "ACCEPT", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Ce délai a déjà été traité.");
    }

    @Test
    void validate_deadlineNotFound_throws404() {
        UUID caseFileId = UUID.randomUUID();
        UUID deadlineId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);
        caseFile.setWorkspace(workspace);
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
        when(deadlineRepository.findById(deadlineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.validate(caseFileId, deadlineId, "ACCEPT", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Deadline not found");
    }
}
