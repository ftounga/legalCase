package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;

class AnalysisDiffServiceTest {

    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

    private final AnalysisDiffService service = new AnalysisDiffService(
            caseAnalysisRepository, caseFileRepository, workspaceMemberRepository, currentUserResolver);

    private User user;
    private Workspace workspace;
    private CaseFile caseFile;
    private final UUID caseFileId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = new User();

        workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        WorkspaceMember member = mock(WorkspaceMember.class);
        when(member.getWorkspace()).thenReturn(workspace);

        caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user))
                .thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId))
                .thenReturn(Optional.of(caseFile));
    }

    // ─── Nominal diff ────────────────────────────────────────────────────────

    @Test
    void diff_nominal_faits_added_removed_unchanged() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        CaseAnalysis from = doneAnalysis(fromId, caseFileId,
                """
                {"faits":["A","B"],"points_juridiques":[],"risques":[],"questions_ouvertes":[],"timeline":[]}
                """);
        CaseAnalysis to = doneAnalysis(toId, caseFileId,
                """
                {"faits":["B","C"],"points_juridiques":[],"risques":[],"questions_ouvertes":[],"timeline":[]}
                """);

        when(caseAnalysisRepository.findByIdAndCaseFileId(fromId, caseFileId)).thenReturn(Optional.of(from));
        when(caseAnalysisRepository.findByIdAndCaseFileId(toId, caseFileId)).thenReturn(Optional.of(to));

        AnalysisDiffResponse result = service.diff(caseFileId, fromId, toId, null, "GOOGLE", null);

        assertThat(result.faits().added()).containsExactly("C");
        assertThat(result.faits().removed()).containsExactly("A");
        assertThat(result.faits().unchanged()).containsExactly("B");
    }

    @Test
    void diff_nominal_timeline_equality_on_date_and_evenement() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        CaseAnalysis from = doneAnalysis(fromId, caseFileId,
                """
                {"faits":[],"points_juridiques":[],"risques":[],"questions_ouvertes":[],
                 "timeline":[{"date":"2024-01-01","evenement":"Embauche"},{"date":"2024-06-01","evenement":"Licenciement"}]}
                """);
        CaseAnalysis to = doneAnalysis(toId, caseFileId,
                """
                {"faits":[],"points_juridiques":[],"risques":[],"questions_ouvertes":[],
                 "timeline":[{"date":"2024-01-01","evenement":"Embauche"},{"date":"2024-07-01","evenement":"Jugement"}]}
                """);

        when(caseAnalysisRepository.findByIdAndCaseFileId(fromId, caseFileId)).thenReturn(Optional.of(from));
        when(caseAnalysisRepository.findByIdAndCaseFileId(toId, caseFileId)).thenReturn(Optional.of(to));

        AnalysisDiffResponse result = service.diff(caseFileId, fromId, toId, null, "GOOGLE", null);

        assertThat(result.timeline().unchanged())
                .extracting(AnalysisDiffResponse.TimelineEntry::evenement)
                .containsExactly("Embauche");
        assertThat(result.timeline().removed())
                .extracting(AnalysisDiffResponse.TimelineEntry::evenement)
                .containsExactly("Licenciement");
        assertThat(result.timeline().added())
                .extracting(AnalysisDiffResponse.TimelineEntry::evenement)
                .containsExactly("Jugement");
    }

    @Test
    void diff_identicalLists_allUnchanged() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        String json = """
                {"faits":["A","B"],"points_juridiques":[],"risques":[],"questions_ouvertes":[],"timeline":[]}
                """;
        CaseAnalysis from = doneAnalysis(fromId, caseFileId, json);
        CaseAnalysis to = doneAnalysis(toId, caseFileId, json);

        when(caseAnalysisRepository.findByIdAndCaseFileId(fromId, caseFileId)).thenReturn(Optional.of(from));
        when(caseAnalysisRepository.findByIdAndCaseFileId(toId, caseFileId)).thenReturn(Optional.of(to));

        AnalysisDiffResponse result = service.diff(caseFileId, fromId, toId, null, "GOOGLE", null);

        assertThat(result.faits().added()).isEmpty();
        assertThat(result.faits().removed()).isEmpty();
        assertThat(result.faits().unchanged()).containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void diff_fromEmpty_allItemsInAdded() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        CaseAnalysis from = doneAnalysis(fromId, caseFileId,
                """
                {"faits":[],"points_juridiques":[],"risques":[],"questions_ouvertes":[],"timeline":[]}
                """);
        CaseAnalysis to = doneAnalysis(toId, caseFileId,
                """
                {"faits":["X","Y"],"points_juridiques":[],"risques":[],"questions_ouvertes":[],"timeline":[]}
                """);

        when(caseAnalysisRepository.findByIdAndCaseFileId(fromId, caseFileId)).thenReturn(Optional.of(from));
        when(caseAnalysisRepository.findByIdAndCaseFileId(toId, caseFileId)).thenReturn(Optional.of(to));

        AnalysisDiffResponse result = service.diff(caseFileId, fromId, toId, null, "GOOGLE", null);

        assertThat(result.faits().added()).containsExactlyInAnyOrder("X", "Y");
        assertThat(result.faits().removed()).isEmpty();
        assertThat(result.faits().unchanged()).isEmpty();
    }

    @Test
    void diff_toEmpty_allItemsInRemoved() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        CaseAnalysis from = doneAnalysis(fromId, caseFileId,
                """
                {"faits":["X","Y"],"points_juridiques":[],"risques":[],"questions_ouvertes":[],"timeline":[]}
                """);
        CaseAnalysis to = doneAnalysis(toId, caseFileId,
                """
                {"faits":[],"points_juridiques":[],"risques":[],"questions_ouvertes":[],"timeline":[]}
                """);

        when(caseAnalysisRepository.findByIdAndCaseFileId(fromId, caseFileId)).thenReturn(Optional.of(from));
        when(caseAnalysisRepository.findByIdAndCaseFileId(toId, caseFileId)).thenReturn(Optional.of(to));

        AnalysisDiffResponse result = service.diff(caseFileId, fromId, toId, null, "GOOGLE", null);

        assertThat(result.faits().removed()).containsExactlyInAnyOrder("X", "Y");
        assertThat(result.faits().added()).isEmpty();
        assertThat(result.faits().unchanged()).isEmpty();
    }

    // ─── Error cases ─────────────────────────────────────────────────────────

    @Test
    void diff_sameFromAndTo_returns400() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.diff(caseFileId, id, id, null, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(BAD_REQUEST);
    }

    @Test
    void diff_analysisNotDone_returns409() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        CaseAnalysis from = doneAnalysis(fromId, caseFileId, emptyJson());
        CaseAnalysis to = analysisWithStatus(toId, caseFileId, AnalysisStatus.PROCESSING, emptyJson());

        when(caseAnalysisRepository.findByIdAndCaseFileId(fromId, caseFileId)).thenReturn(Optional.of(from));
        when(caseAnalysisRepository.findByIdAndCaseFileId(toId, caseFileId)).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> service.diff(caseFileId, fromId, toId, null, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(CONFLICT);
    }

    @Test
    void diff_analysisFromAnotherCaseFile_returns404() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        when(caseAnalysisRepository.findByIdAndCaseFileId(fromId, caseFileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.diff(caseFileId, fromId, toId, null, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(NOT_FOUND);
    }

    @Test
    void diff_caseFileFromAnotherWorkspace_returns404() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setId(UUID.randomUUID());
        CaseFile otherCaseFile = new CaseFile();
        otherCaseFile.setWorkspace(otherWorkspace);

        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId))
                .thenReturn(Optional.of(otherCaseFile));

        assertThatThrownBy(() -> service.diff(caseFileId, fromId, toId, null, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(NOT_FOUND);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private CaseAnalysis doneAnalysis(UUID id, UUID caseFileId, String json) {
        return analysisWithStatus(id, caseFileId, AnalysisStatus.DONE, json);
    }

    private CaseAnalysis analysisWithStatus(UUID id, UUID caseFileId, AnalysisStatus status, String json) {
        CaseFile cf = new CaseFile();
        cf.setWorkspace(workspace);

        CaseAnalysis a = new CaseAnalysis();
        a.setId(id);
        a.setCaseFile(cf);
        a.setAnalysisStatus(status);
        a.setAnalysisType(AnalysisType.STANDARD);
        a.setAnalysisResult(json);
        a.setVersion(1);
        a.setUpdatedAt(Instant.now());
        return a;
    }

    private String emptyJson() {
        return """
                {"faits":[],"points_juridiques":[],"risques":[],"questions_ouvertes":[],"timeline":[]}
                """;
    }
}
