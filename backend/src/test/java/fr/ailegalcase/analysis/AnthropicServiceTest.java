package fr.ailegalcase.analysis;

import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.shared.PaymentRequiredCode;
import fr.ailegalcase.shared.PaymentRequiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AnthropicServiceTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CASE_FILE_ID = UUID.randomUUID();

    private static final String OK_RESPONSE = """
            {
              "content": [{"type": "text", "text": "{\\"faits\\": [\\"fait1\\"]}"}],
              "model": "claude-sonnet-4-6-20241022",
              "stop_reason": "end_turn",
              "usage": {"input_tokens": 150, "output_tokens": 80}
            }
            """;

    private MockRestServiceServer server;
    private AnthropicService service;
    private PlanLimitService planLimitService;
    private UsageEventService usageEventService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        planLimitService = mock(PlanLimitService.class);
        usageEventService = mock(UsageEventService.class);
        service = new AnthropicService("claude-sonnet-4-6", "claude-haiku-4-5-20251001",
                builder, planLimitService, usageEventService);
    }

    private AiCallContext userCtx(JobType jobType) {
        return AiCallContext.userLevel(WORKSPACE_ID, USER_ID, CASE_FILE_ID, jobType);
    }

    // ── analyzeChunk ─────────────────────────────────────────────────────

    @Test
    void analyzeChunk_userLevel_gateOk_callsApiAndRecords() {
        when(planLimitService.isMonthlyTokenBudgetExceeded(WORKSPACE_ID)).thenReturn(false);
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(OK_RESPONSE, MediaType.APPLICATION_JSON));

        AnthropicResult result = service.analyzeChunk(
                userCtx(JobType.CHUNK_ANALYSIS),
                "Texte juridique de test.", "DROIT_DU_TRAVAIL", "FRANCE");

        assertThat(result.promptTokens()).isEqualTo(150);
        assertThat(result.completionTokens()).isEqualTo(80);
        verify(usageEventService).record(CASE_FILE_ID, USER_ID, JobType.CHUNK_ANALYSIS, 150, 80);
        server.verify();
    }

    @Test
    void analyzeChunk_emptyText_throwsAndDoesNotCallApi() {
        when(planLimitService.isMonthlyTokenBudgetExceeded(WORKSPACE_ID)).thenReturn(false);
        assertThatThrownBy(() -> service.analyzeChunk(
                userCtx(JobType.CHUNK_ANALYSIS), "", "DROIT_DU_TRAVAIL", "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(usageEventService);
    }

    @Test
    void analyzeChunk_nullText_throwsAndDoesNotCallApi() {
        when(planLimitService.isMonthlyTokenBudgetExceeded(WORKSPACE_ID)).thenReturn(false);
        assertThatThrownBy(() -> service.analyzeChunk(
                userCtx(JobType.CHUNK_ANALYSIS), null, "DROIT_DU_TRAVAIL", "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(usageEventService);
    }

    @Test
    void analyzeChunk_blankText_throwsAndDoesNotCallApi() {
        when(planLimitService.isMonthlyTokenBudgetExceeded(WORKSPACE_ID)).thenReturn(false);
        assertThatThrownBy(() -> service.analyzeChunk(
                userCtx(JobType.CHUNK_ANALYSIS), "   ", "DROIT_DU_TRAVAIL", "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(usageEventService);
    }

    // ── Gate token user-level (F-257) ────────────────────────────────────

    @Test
    void analyze_userLevel_gateExceeded_throwsPaymentRequiredAndDoesNotCallApi() {
        when(planLimitService.isMonthlyTokenBudgetExceeded(WORKSPACE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.analyze(
                userCtx(JobType.CASE_ANALYSIS), "system", "user", 1024))
                .isInstanceOf(PaymentRequiredException.class)
                .matches(e -> ((PaymentRequiredException) e).getCode() == PaymentRequiredCode.TOKEN_BUDGET_EXCEEDED);

        // Aucune requête HTTP n'a été émise — MockRestServiceServer aurait failli sinon.
        server.verify();
        verifyNoInteractions(usageEventService);
    }

    @Test
    void analyzeFast_userLevel_gateExceeded_throwsAndDoesNotCallApi() {
        when(planLimitService.isMonthlyTokenBudgetExceeded(WORKSPACE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.analyzeFast(
                userCtx(JobType.QUESTION_GENERATION), "system", "user", 512))
                .isInstanceOf(PaymentRequiredException.class);

        server.verify();
        verifyNoInteractions(usageEventService);
    }

    @Test
    void analyzeWithSystemCacheStreaming_userLevel_gateExceeded_throwsBeforeStream() {
        when(planLimitService.isMonthlyTokenBudgetExceeded(WORKSPACE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.analyzeWithSystemCacheStreaming(
                userCtx(JobType.ENRICHED_ANALYSIS), "system", "user", 4096, null))
                .isInstanceOf(PaymentRequiredException.class);

        server.verify();
        verifyNoInteractions(usageEventService);
    }

    // ── System-level (skip gate, record obligatoire) ─────────────────────

    @Test
    void analyzeFast_systemLevel_skipsGateAndRecordsWithNullUserId() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(OK_RESPONSE, MediaType.APPLICATION_JSON));

        AiCallContext systemCtx = AiCallContext.systemLevel(JobType.SYSTEM_HELP_CHAT);
        AnthropicResult result = service.analyzeFast(systemCtx, "help system prompt", "question", 512);

        assertThat(result.promptTokens()).isEqualTo(150);
        // userId est null pour system-level, caseFileId aussi
        verify(usageEventService).record(null, null, JobType.SYSTEM_HELP_CHAT, 150, 80);
        // Le gate user n'a JAMAIS été interrogé.
        verifyNoInteractions(planLimitService);
        server.verify();
    }

    @Test
    void analyzeWithModel_systemLevel_withCaseFileId_recordsWithCaseFile() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(OK_RESPONSE, MediaType.APPLICATION_JSON));

        AiCallContext systemCtx = AiCallContext.systemLevel(JobType.SYSTEM_CASE_CONCLUSION, CASE_FILE_ID);
        service.analyzeWithModel(systemCtx, "claude-sonnet-4-6", "system", "user", 8000);

        verify(usageEventService).record(CASE_FILE_ID, null, JobType.SYSTEM_CASE_CONCLUSION, 150, 80);
        verifyNoInteractions(planLimitService);
        server.verify();
    }

    // ── AiCallContext validation ─────────────────────────────────────────

    @Test
    void aiCallContext_userLevel_workspaceIdNull_throws() {
        assertThatThrownBy(() -> AiCallContext.userLevel(null, USER_ID, CASE_FILE_ID, JobType.CASE_ANALYSIS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workspaceId");
    }

    @Test
    void aiCallContext_userLevel_userIdNull_throws() {
        assertThatThrownBy(() -> AiCallContext.userLevel(WORKSPACE_ID, null, CASE_FILE_ID, JobType.CASE_ANALYSIS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void aiCallContext_jobTypeNull_throws() {
        assertThatThrownBy(() -> new AiCallContext(WORKSPACE_ID, USER_ID, CASE_FILE_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jobType");
    }

    @Test
    void aiCallContext_systemLevel_acceptsAllNullExceptJobType() {
        // Aucune exception attendue.
        AiCallContext ctx = AiCallContext.systemLevel(JobType.SYSTEM_BLOG_GENERATION);
        assertThat(ctx.workspaceId()).isNull();
        assertThat(ctx.userId()).isNull();
        assertThat(ctx.caseFileId()).isNull();
        assertThat(ctx.jobType()).isEqualTo(JobType.SYSTEM_BLOG_GENERATION);
    }

    // ── JobType isUserLevel / isSystemLevel ──────────────────────────────

    @Test
    void jobType_userLevel_classification() {
        assertThat(JobType.CHUNK_ANALYSIS.isUserLevel()).isTrue();
        assertThat(JobType.DOCUMENT_ANALYSIS.isUserLevel()).isTrue();
        assertThat(JobType.CASE_ANALYSIS.isUserLevel()).isTrue();
        assertThat(JobType.QUESTION_GENERATION.isUserLevel()).isTrue();
        assertThat(JobType.ENRICHED_ANALYSIS.isUserLevel()).isTrue();
        assertThat(JobType.CHAT_MESSAGE.isUserLevel()).isTrue();
        for (JobType jt : List.of(JobType.CHUNK_ANALYSIS, JobType.DOCUMENT_ANALYSIS, JobType.CASE_ANALYSIS,
                JobType.QUESTION_GENERATION, JobType.ENRICHED_ANALYSIS, JobType.CHAT_MESSAGE)) {
            assertThat(jt.isSystemLevel()).isFalse();
        }
    }

    @Test
    void jobType_systemLevel_classification() {
        for (JobType jt : List.of(JobType.SYSTEM_REFERENTIAL_CHECK, JobType.SYSTEM_BLOG_GENERATION,
                JobType.SYSTEM_JP_BOOTSTRAP, JobType.SYSTEM_HELP_CHAT, JobType.SYSTEM_VISION_ENRICHMENT,
                JobType.SYSTEM_PIECE_DETECTION, JobType.SYSTEM_STYLE_LEARNING, JobType.SYSTEM_CASE_CONCLUSION,
                JobType.SYSTEM_SEMANTIC_DIFF, JobType.SYSTEM_JURISPRUDENCE_VERIFICATION,
                JobType.SYSTEM_CHAT_SUMMARY)) {
            assertThat(jt.isSystemLevel()).as(jt.name()).isTrue();
            assertThat(jt.isUserLevel()).as(jt.name()).isFalse();
        }
    }

    // ── Retry HTTP 5xx ───────────────────────────────────────────────────

    @Test
    void doAnalyze_serverError_then_success_recordsOnceWithFinalTokens() throws Exception {
        when(planLimitService.isMonthlyTokenBudgetExceeded(WORKSPACE_ID)).thenReturn(false);
        // Le service fait Thread.sleep(5s) entre 5xx et retry — pour rester rapide en CI,
        // on déclenche un 503 puis un succès, et on accepte la latence du premier backoff (~5s).
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(OK_RESPONSE, MediaType.APPLICATION_JSON));

        AnthropicResult result = service.analyze(
                userCtx(JobType.CASE_ANALYSIS), "system", "user", 1024);

        assertThat(result.promptTokens()).isEqualTo(150);
        verify(usageEventService, times(1)).record(eq(CASE_FILE_ID), eq(USER_ID),
                eq(JobType.CASE_ANALYSIS), eq(150), eq(80));
        server.verify();
    }

    @Test
    void doAnalyze_serverError_persistent_propagatesAndDoesNotRecord() {
        when(planLimitService.isMonthlyTokenBudgetExceeded(WORKSPACE_ID)).thenReturn(false);
        // 5 tentatives consécutives 500 (1 initial + 4 retries de backoff).
        for (int i = 0; i < 5; i++) {
            server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withServerError());
        }

        assertThatThrownBy(() -> service.analyze(
                userCtx(JobType.CASE_ANALYSIS), "system", "user", 1024))
                .isInstanceOf(org.springframework.web.client.HttpServerErrorException.class);

        verify(usageEventService, never()).record(any(), any(), any(), anyInt(), anyInt());
        server.verify();
    }

    // ── Isolation workspace (gate ciblé) ─────────────────────────────────

    @Test
    void doAnalyze_userLevel_gateCheckedOnContextWorkspaceOnly() {
        when(planLimitService.isMonthlyTokenBudgetExceeded(WORKSPACE_ID)).thenReturn(false);
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(OK_RESPONSE, MediaType.APPLICATION_JSON));

        service.analyze(userCtx(JobType.CASE_ANALYSIS), "system", "user", 1024);

        ArgumentCaptor<UUID> wsCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(planLimitService).isMonthlyTokenBudgetExceeded(wsCaptor.capture());
        assertThat(wsCaptor.getValue()).isEqualTo(WORKSPACE_ID);
        server.verify();
    }
}
