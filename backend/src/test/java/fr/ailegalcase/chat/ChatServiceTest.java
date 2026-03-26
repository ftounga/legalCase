package fr.ailegalcase.chat;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.UsageEventService;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private CaseFileRepository caseFileRepository;
    @Mock private CaseAnalysisRepository caseAnalysisRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private AnthropicService anthropicService;
    @Mock private UsageEventService usageEventService;
    @Mock private PlanLimitService planLimitService;

    private ChatService service;

    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CASE_FILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID      = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @BeforeEach
    void setUp() {
        service = new ChatService(chatMessageRepository, caseFileRepository, caseAnalysisRepository,
                workspaceMemberRepository, currentUserResolver, anthropicService,
                usageEventService, planLimitService);
    }

    private void mockContext(boolean budgetExceeded, boolean hasSynthesis) {
        User user = new User();
        user.setId(USER_ID);

        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);

        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CASE_FILE_ID)).thenReturn(Optional.of(caseFile));
        when(planLimitService.isChatMessageLimitReached(WORKSPACE_ID)).thenReturn(budgetExceeded);

        if (!budgetExceeded && hasSynthesis) {
            CaseAnalysis analysis = new CaseAnalysis();
            analysis.setAnalysisResult("{\"synthese\":\"test\"}");
            analysis.setAnalysisStatus(AnalysisStatus.DONE);
            when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                    CASE_FILE_ID, AnalysisStatus.DONE)).thenReturn(Optional.of(analysis));
        } else if (!budgetExceeded) {
            when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                    CASE_FILE_ID, AnalysisStatus.DONE)).thenReturn(Optional.empty());
        }
    }

    // U-01 : limite non atteinte, synthèse présente → message créé, réponse retournée
    @Test
    void sendMessage_success_returnsResponse() {
        mockContext(false, true);
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("Réponse IA", "claude-haiku-4-5-20251001", 100, 50));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageResponse response = service.sendMessage(
                CASE_FILE_ID, new ChatMessageRequest("Question test", false), null, "GOOGLE", null);

        assertThat(response.answer()).isEqualTo("Réponse IA");
        assertThat(response.modelUsed()).isEqualTo("claude-haiku-4-5-20251001");
        verify(usageEventService).record(eq(CASE_FILE_ID), eq(USER_ID), any(), eq(100), eq(50));
    }

    // U-02 : limite atteinte → 402
    @Test
    void sendMessage_limitReached_throws402() {
        mockContext(true, false);

        assertThatThrownBy(() -> service.sendMessage(
                CASE_FILE_ID, new ChatMessageRequest("Question", false), null, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(PAYMENT_REQUIRED));

        verifyNoInteractions(anthropicService);
    }

    // U-03 : pas de synthèse → 424
    @Test
    void sendMessage_noSynthesis_throws424() {
        mockContext(false, false);

        assertThatThrownBy(() -> service.sendMessage(
                CASE_FILE_ID, new ChatMessageRequest("Question", false), null, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(FAILED_DEPENDENCY));

        verifyNoInteractions(anthropicService);
    }

    // U-04 : useEnriched=true + PRO → modèle Sonnet (analyze appelé, pas analyzeFast)
    @Test
    void sendMessage_useEnriched_pro_callsSonnet() {
        mockContext(false, true);
        when(planLimitService.isEnrichedAnalysisAllowedForWorkspace(WORKSPACE_ID)).thenReturn(true);
        when(anthropicService.analyze(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("Réponse Sonnet", "claude-sonnet-4-6", 200, 100));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageResponse response = service.sendMessage(
                CASE_FILE_ID, new ChatMessageRequest("Analyse approfondie", true), null, "GOOGLE", null);

        assertThat(response.useEnriched()).isTrue();
        verify(anthropicService).analyze(any(), any(), anyInt());
        verify(anthropicService, never()).analyzeFast(any(), any(), anyInt());
    }

    // U-05 : useEnriched=true + STARTER → fallback Haiku
    @Test
    void sendMessage_useEnriched_starter_fallbackToHaiku() {
        mockContext(false, true);
        when(planLimitService.isEnrichedAnalysisAllowedForWorkspace(WORKSPACE_ID)).thenReturn(false);
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("Réponse Haiku", "claude-haiku-4-5-20251001", 100, 50));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageResponse response = service.sendMessage(
                CASE_FILE_ID, new ChatMessageRequest("Question", true), null, "GOOGLE", null);

        assertThat(response.useEnriched()).isFalse();
        verify(anthropicService).analyzeFast(any(), any(), anyInt());
        verify(anthropicService, never()).analyze(any(), any(), anyInt());
    }

    // U-06 : sans souscription → autorisé (isChatMessageLimitReached retourne false)
    @Test
    void sendMessage_noSubscription_allowed() {
        mockContext(false, true);
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("OK", "claude-haiku-4-5-20251001", 50, 25));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageResponse response = service.sendMessage(
                CASE_FILE_ID, new ChatMessageRequest("Question", false), null, "GOOGLE", null);

        assertThat(response.answer()).isEqualTo("OK");
    }
}
