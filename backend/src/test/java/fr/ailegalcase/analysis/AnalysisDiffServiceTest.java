package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final AnalysisDiffCacheRepository analysisDiffCacheRepository = mock(AnalysisDiffCacheRepository.class);
    private final SemanticDiffService semanticDiffService = mock(SemanticDiffService.class);
    private final AnalysisDocumentRepository analysisDocumentRepository = mock(AnalysisDocumentRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final AnalysisDiffService service = new AnalysisDiffService(
            caseAnalysisRepository, caseFileRepository, workspaceMemberRepository,
            currentUserResolver, analysisDiffCacheRepository, semanticDiffService,
            analysisDocumentRepository, objectMapper);

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
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
        when(analysisDiffCacheRepository.findByFromIdAndToId(any(), any())).thenReturn(Optional.empty());
        when(analysisDocumentRepository.findByAnalysisIdOrderByCreatedAt(any())).thenReturn(List.of());
    }

    // ─── Cache ───────────────────────────────────────────────────────────────

    @Test
    void diff_cacheMiss_callsSemanticDiffAndPersistsCache() throws Exception {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        CaseAnalysis from = doneAnalysis(fromId, caseFileId, emptyJson());
        CaseAnalysis to = doneAnalysis(toId, caseFileId, emptyJson());
        when(caseAnalysisRepository.findByIdAndCaseFileId(fromId, caseFileId)).thenReturn(Optional.of(from));
        when(caseAnalysisRepository.findByIdAndCaseFileId(toId, caseFileId)).thenReturn(Optional.of(to));

        AnalysisDiffResponse fakeResponse = emptyDiffResponse(fromId, toId);
        when(semanticDiffService.diff(from, to, List.of(), List.of())).thenReturn(fakeResponse);
        when(analysisDiffCacheRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.diff(caseFileId, fromId, toId, null, "GOOGLE", null);

        verify(semanticDiffService).diff(from, to, List.of(), List.of());
        verify(analysisDiffCacheRepository).save(any());
    }

    @Test
    void diff_cacheHit_doesNotCallSemanticDiff() throws Exception {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        CaseAnalysis from = doneAnalysis(fromId, caseFileId, emptyJson());
        CaseAnalysis to = doneAnalysis(toId, caseFileId, emptyJson());
        when(caseAnalysisRepository.findByIdAndCaseFileId(fromId, caseFileId)).thenReturn(Optional.of(from));
        when(caseAnalysisRepository.findByIdAndCaseFileId(toId, caseFileId)).thenReturn(Optional.of(to));

        AnalysisDiffResponse fakeResponse = emptyDiffResponse(fromId, toId);
        AnalysisDiffCache cached = new AnalysisDiffCache();
        cached.setResultJson(objectMapper.writeValueAsString(fakeResponse));
        when(analysisDiffCacheRepository.findByFromIdAndToId(fromId, toId)).thenReturn(Optional.of(cached));

        AnalysisDiffResponse result = service.diff(caseFileId, fromId, toId, null, "GOOGLE", null);

        verify(semanticDiffService, never()).diff(any(), any(), any(), any());
        assertThat(result.from().id()).isEqualTo(fromId);
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
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(otherCaseFile));

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

    private AnalysisDiffResponse emptyDiffResponse(UUID fromId, UUID toId) {
        AnalysisDiffResponse.VersionInfo from = new AnalysisDiffResponse.VersionInfo(fromId, 1, "STANDARD", Instant.now());
        AnalysisDiffResponse.VersionInfo to = new AnalysisDiffResponse.VersionInfo(toId, 2, "STANDARD", Instant.now());
        AnalysisDiffResponse.SectionDiff empty = new AnalysisDiffResponse.SectionDiff(
                List.of(), List.of(), List.of(), List.of());
        AnalysisDiffResponse.TimelineSectionDiff emptyTimeline = new AnalysisDiffResponse.TimelineSectionDiff(
                List.of(), List.of(), List.of(), List.of());
        return new AnalysisDiffResponse(from, to, empty, empty, empty, empty, emptyTimeline);
    }
}
