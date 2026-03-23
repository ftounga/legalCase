package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CaseAnalysisCommandServiceTest {

    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final DocumentAnalysisRepository documentAnalysisRepository = mock(DocumentAnalysisRepository.class);
    private final AnalysisJobRepository analysisJobRepository = mock(AnalysisJobRepository.class);
    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final PlanLimitService planLimitService = mock(PlanLimitService.class);

    private final CaseAnalysisCommandService service = new CaseAnalysisCommandService(
            caseFileRepository, caseAnalysisRepository, documentAnalysisRepository,
            analysisJobRepository, currentUserResolver, workspaceMemberRepository,
            rabbitTemplate, planLimitService);

    // C-01 : nominal — message RabbitMQ publié
    @Test
    void triggerCaseAnalysis_nominal_publishesMessage() {
        var ctx = buildContext();
        when(caseAnalysisRepository.existsByCaseFileIdAndAnalysisStatusIn(eq(ctx.caseFileId()), any())).thenReturn(false);
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(ctx.caseFileId(), AnalysisStatus.DONE)).thenReturn(2L);
        when(planLimitService.isCaseAnalysisLimitReached(ctx.caseFileId(), ctx.workspaceId())).thenReturn(false);
        when(planLimitService.isMonthlyTokenBudgetExceeded(ctx.workspaceId())).thenReturn(false);
        when(analysisJobRepository.findByCaseFileIdAndJobType(ctx.caseFileId(), JobType.CASE_ANALYSIS)).thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.triggerCaseAnalysis(ctx.caseFileId(), null, null, null);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.CASE_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.CASE_ANALYSIS_ROUTING_KEY),
                any(CaseAnalysisMessage.class));
    }

    // C-02 : dossier inconnu ou autre workspace → 404
    @Test
    void triggerCaseAnalysis_unknownCaseFile_throws404() {
        var ctx = buildContext();
        when(caseFileRepository.findById(ctx.caseFileId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.triggerCaseAnalysis(ctx.caseFileId(), null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // C-03 : analyse déjà en cours → 409
    @Test
    void triggerCaseAnalysis_analysisAlreadyRunning_throws409() {
        var ctx = buildContext();
        when(caseAnalysisRepository.existsByCaseFileIdAndAnalysisStatusIn(eq(ctx.caseFileId()), any())).thenReturn(true);

        assertThatThrownBy(() -> service.triggerCaseAnalysis(ctx.caseFileId(), null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    // C-04 : aucun document DONE → 422
    @Test
    void triggerCaseAnalysis_noDocumentDone_throws422() {
        var ctx = buildContext();
        when(caseAnalysisRepository.existsByCaseFileIdAndAnalysisStatusIn(eq(ctx.caseFileId()), any())).thenReturn(false);
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(ctx.caseFileId(), AnalysisStatus.DONE)).thenReturn(0L);

        assertThatThrownBy(() -> service.triggerCaseAnalysis(ctx.caseFileId(), null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
    }

    // C-05 : limite analyses atteinte → 402
    @Test
    void triggerCaseAnalysis_limitReached_throws402() {
        var ctx = buildContext();
        when(caseAnalysisRepository.existsByCaseFileIdAndAnalysisStatusIn(eq(ctx.caseFileId()), any())).thenReturn(false);
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(ctx.caseFileId(), AnalysisStatus.DONE)).thenReturn(1L);
        when(planLimitService.isCaseAnalysisLimitReached(ctx.caseFileId(), ctx.workspaceId())).thenReturn(true);

        assertThatThrownBy(() -> service.triggerCaseAnalysis(ctx.caseFileId(), null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("402");
    }

    // C-06 : budget tokens dépassé → 402
    @Test
    void triggerCaseAnalysis_tokenBudgetExceeded_throws402() {
        var ctx = buildContext();
        when(caseAnalysisRepository.existsByCaseFileIdAndAnalysisStatusIn(eq(ctx.caseFileId()), any())).thenReturn(false);
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(ctx.caseFileId(), AnalysisStatus.DONE)).thenReturn(1L);
        when(planLimitService.isCaseAnalysisLimitReached(ctx.caseFileId(), ctx.workspaceId())).thenReturn(false);
        when(planLimitService.isMonthlyTokenBudgetExceeded(ctx.workspaceId())).thenReturn(true);

        assertThatThrownBy(() -> service.triggerCaseAnalysis(ctx.caseFileId(), null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("402");
    }

    // C-07 : isolation workspace — dossier d'un autre workspace → 404
    @Test
    void triggerCaseAnalysis_caseFileBelongsToOtherWorkspace_throws404() {
        var ctx = buildContext();
        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setId(UUID.randomUUID());

        CaseFile caseFileOther = new CaseFile();
        caseFileOther.setId(ctx.caseFileId());
        caseFileOther.setWorkspace(otherWorkspace);
        when(caseFileRepository.findById(ctx.caseFileId())).thenReturn(Optional.of(caseFileOther));

        assertThatThrownBy(() -> service.triggerCaseAnalysis(ctx.caseFileId(), null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // C-08 : re-analyse — job QUESTION_GENERATION existant remis à PENDING
    @Test
    void triggerCaseAnalysis_resetsExistingQuestionGenerationJob() {
        var ctx = buildContext();
        when(caseAnalysisRepository.existsByCaseFileIdAndAnalysisStatusIn(eq(ctx.caseFileId()), any())).thenReturn(false);
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(ctx.caseFileId(), AnalysisStatus.DONE)).thenReturn(1L);
        when(planLimitService.isCaseAnalysisLimitReached(ctx.caseFileId(), ctx.workspaceId())).thenReturn(false);
        when(planLimitService.isMonthlyTokenBudgetExceeded(ctx.workspaceId())).thenReturn(false);
        when(analysisJobRepository.findByCaseFileIdAndJobType(ctx.caseFileId(), JobType.CASE_ANALYSIS)).thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AnalysisJob existingQgJob = new AnalysisJob();
        existingQgJob.setCaseFileId(ctx.caseFileId());
        existingQgJob.setJobType(JobType.QUESTION_GENERATION);
        existingQgJob.setStatus(AnalysisStatus.DONE);
        existingQgJob.setProcessedItems(1);
        existingQgJob.setTotalItems(1);
        when(analysisJobRepository.findByCaseFileIdAndJobType(ctx.caseFileId(), JobType.QUESTION_GENERATION))
                .thenReturn(Optional.of(existingQgJob));

        service.triggerCaseAnalysis(ctx.caseFileId(), null, null, null);

        org.assertj.core.api.Assertions.assertThat(existingQgJob.getStatus()).isEqualTo(AnalysisStatus.PENDING);
        org.assertj.core.api.Assertions.assertThat(existingQgJob.getProcessedItems()).isZero();
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private record Ctx(UUID caseFileId, UUID workspaceId) {}

    private Ctx buildContext() {
        User user = new User();
        user.setId(UUID.randomUUID());

        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());

        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);
        caseFile.setWorkspace(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));

        return new Ctx(caseFileId, workspace.getId());
    }
}
