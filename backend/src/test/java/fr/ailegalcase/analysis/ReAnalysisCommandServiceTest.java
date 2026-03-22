package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.PAYMENT_REQUIRED;

@ExtendWith(MockitoExtension.class)
class ReAnalysisCommandServiceTest {

    @Mock private CaseFileRepository caseFileRepository;
    @Mock private AnalysisJobRepository analysisJobRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private PlanLimitService planLimitService;
    @Mock private OidcUser oidcUser;

    private ReAnalysisCommandService service;

    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CASE_FILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @BeforeEach
    void setUp() {
        service = new ReAnalysisCommandService(caseFileRepository, analysisJobRepository,
                currentUserResolver, workspaceMemberRepository, rabbitTemplate, planLimitService);
    }

    private void mockUserWorkspaceAndCaseFile() {
        User user = new User();
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findById(CASE_FILE_ID)).thenReturn(Optional.of(caseFile));
        lenient().when(analysisJobRepository.findByCaseFileIdAndJobType(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // U-01 : plan PRO, sous la limite → re-analyse autorisée, RabbitMQ publié
    @Test
    void triggerReAnalysis_proPlan_publishesMessage() {
        mockUserWorkspaceAndCaseFile();
        when(planLimitService.isEnrichedAnalysisAllowedForWorkspace(WORKSPACE_ID)).thenReturn(true);
        when(planLimitService.isReAnalysisLimitReached(CASE_FILE_ID, WORKSPACE_ID)).thenReturn(false);

        service.triggerReAnalysis(CASE_FILE_ID, oidcUser, "GOOGLE", null);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.RE_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.RE_ANALYSIS_ROUTING_KEY),
                any(ReAnalysisMessage.class));
    }

    // U-02 : plan STARTER → 402, RabbitMQ non publié
    @Test
    void triggerReAnalysis_starterPlan_throws402() {
        mockUserWorkspaceAndCaseFile();
        when(planLimitService.isEnrichedAnalysisAllowedForWorkspace(WORKSPACE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.triggerReAnalysis(CASE_FILE_ID, oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    var rse = (ResponseStatusException) ex;
                    assert rse.getStatusCode() == PAYMENT_REQUIRED;
                });

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    // U-02b : plan PRO, limite atteinte → 402, RabbitMQ non publié
    @Test
    void triggerReAnalysis_proAtLimit_throws402() {
        mockUserWorkspaceAndCaseFile();
        when(planLimitService.isEnrichedAnalysisAllowedForWorkspace(WORKSPACE_ID)).thenReturn(true);
        when(planLimitService.isReAnalysisLimitReached(CASE_FILE_ID, WORKSPACE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.triggerReAnalysis(CASE_FILE_ID, oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    var rse = (ResponseStatusException) ex;
                    assert rse.getStatusCode() == PAYMENT_REQUIRED;
                });

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    // U-03 : pas de subscription → fail open, RabbitMQ publié
    @Test
    void triggerReAnalysis_noSubscription_failOpen() {
        mockUserWorkspaceAndCaseFile();
        when(planLimitService.isEnrichedAnalysisAllowedForWorkspace(WORKSPACE_ID)).thenReturn(true);
        when(planLimitService.isReAnalysisLimitReached(CASE_FILE_ID, WORKSPACE_ID)).thenReturn(false);

        service.triggerReAnalysis(CASE_FILE_ID, oidcUser, "GOOGLE", null);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.RE_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.RE_ANALYSIS_ROUTING_KEY),
                any(ReAnalysisMessage.class));
    }
}
