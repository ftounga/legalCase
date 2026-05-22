package fr.ailegalcase.jurisprudencemapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JurisprudenceWatchServiceTest {

    private ToolJurisprudenceMappingRepository mappingRepo;
    private JurisprudenceWatchFlagRepository flagRepo;
    private JurisprudenceAuditLogRepository auditRepo;
    private JudilibreApiClient judilibre;
    private ClaudeJurisprudenceEvaluator evaluator;
    private JurisprudenceWatchEmailService emailService;

    @BeforeEach
    void setUp() {
        mappingRepo = mock(ToolJurisprudenceMappingRepository.class);
        flagRepo = mock(JurisprudenceWatchFlagRepository.class);
        auditRepo = mock(JurisprudenceAuditLogRepository.class);
        judilibre = mock(JudilibreApiClient.class);
        evaluator = mock(ClaudeJurisprudenceEvaluator.class);
        emailService = mock(JurisprudenceWatchEmailService.class);
    }

    private JurisprudenceWatchService build(String trustMode, int alertThreshold) {
        return new JurisprudenceWatchService(mappingRepo, flagRepo, auditRepo,
                judilibre, evaluator, emailService, trustMode, alertThreshold);
    }

    @Test
    void runForPeriod_noMappings_sendsRecapWithZeros() {
        when(judilibre.fetchArretsForPeriod(any(), any())).thenReturn(List.of());
        when(mappingRepo.findAll()).thenReturn(List.of());

        JurisprudenceWatchService svc = build("AUTO_PILOT", 5);
        JurisprudenceWatchRunSummary summary = svc.runForPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        assertThat(summary.mappingsEvalues()).isZero();
        assertThat(summary.aborted()).isFalse();
        verify(emailService).sendMonthlyRecap(any());
    }

    @Test
    void dispatch_autoPilot_highConfidenceConfirm_appliesAndLogs() {
        JurisprudenceWatchService svc = build("AUTO_PILOT", 5);
        ToolJurisprudenceMapping mapping = buildMapping();
        ClaudeEvaluation eval = new ClaudeEvaluation(EvaluationAction.CONFIRM, null, new BigDecimal("0.95"), "OK");

        JurisprudenceWatchService.DispatchResult result = svc.dispatch(mapping, eval);

        assertThat(result).isEqualTo(JurisprudenceWatchService.DispatchResult.AUTO_CONFIRM);
        verify(mappingRepo).save(mapping);
        verify(auditRepo).save(any(JurisprudenceAuditLog.class));
        verify(flagRepo, never()).save(any());
    }

    @Test
    void dispatch_autoPilot_lowConfidenceConfirm_createsPendingFlag() {
        JurisprudenceWatchService svc = build("AUTO_PILOT", 5);
        ToolJurisprudenceMapping mapping = buildMapping();
        ClaudeEvaluation eval = new ClaudeEvaluation(EvaluationAction.CONFIRM, null, new BigDecimal("0.70"), "doute");

        JurisprudenceWatchService.DispatchResult result = svc.dispatch(mapping, eval);

        assertThat(result).isEqualTo(JurisprudenceWatchService.DispatchResult.FLAG_PENDING);
        verify(flagRepo).save(any(JurisprudenceWatchFlag.class));
        verify(mappingRepo, never()).save(any());
    }

    @Test
    void dispatch_paranoia_alwaysCreatesPendingFlag() {
        JurisprudenceWatchService svc = build("PARANOIA", 5);
        ToolJurisprudenceMapping mapping = buildMapping();
        ClaudeEvaluation eval = new ClaudeEvaluation(EvaluationAction.CONFIRM, null, new BigDecimal("0.99"), "");

        JurisprudenceWatchService.DispatchResult result = svc.dispatch(mapping, eval);

        assertThat(result).isEqualTo(JurisprudenceWatchService.DispatchResult.FLAG_PENDING);
        verify(flagRepo).save(any(JurisprudenceWatchFlag.class));
        verify(mappingRepo, never()).save(any());
    }

    @Test
    void dispatch_silence_belowPendingThreshold_returnsSilence() {
        JurisprudenceWatchService svc = build("AUTO_PILOT", 5);
        ToolJurisprudenceMapping mapping = buildMapping();
        ClaudeEvaluation eval = new ClaudeEvaluation(EvaluationAction.CONFIRM, null, new BigDecimal("0.40"), "weak");

        JurisprudenceWatchService.DispatchResult result = svc.dispatch(mapping, eval);

        assertThat(result).isEqualTo(JurisprudenceWatchService.DispatchResult.SILENCE);
        verify(mappingRepo, never()).save(any());
        verify(flagRepo, never()).save(any());
    }

    @Test
    void dispatch_replace_appliesArchiveAndCreatesNewMapping() {
        JurisprudenceWatchService svc = build("AUTO_PILOT", 5);
        ToolJurisprudenceMapping current = buildMapping();
        JudilibreArret incoming = buildArret("INC1");
        ClaudeEvaluation eval = new ClaudeEvaluation(EvaluationAction.REPLACE, incoming, new BigDecimal("0.90"), "revirement");

        JurisprudenceWatchService.DispatchResult result = svc.dispatch(current, eval);

        assertThat(result).isEqualTo(JurisprudenceWatchService.DispatchResult.AUTO_REPLACE);
        assertThat(current.isArchived()).isTrue();
        verify(mappingRepo, times(2)).save(any(ToolJurisprudenceMapping.class));
    }

    @Test
    void runForPeriod_massiveImpact_abortsAndSendsAlert() {
        // 10 mappings, threshold 5 % → 1 REPLACE = 1*100=100 > 5*10=50 → abort immediately
        List<ToolJurisprudenceMapping> mappings = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            mappings.add(buildMapping());
        }
        when(judilibre.fetchArretsForPeriod(any(), any())).thenReturn(List.of(buildArret("INC1")));
        when(mappingRepo.findAll()).thenReturn(mappings);
        when(evaluator.evaluate(any(), any()))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.REPLACE, buildArret("INC1"),
                        new BigDecimal("0.95"), "all-replace"));

        JurisprudenceWatchService svc = build("AUTO_PILOT", 5);
        JurisprudenceWatchRunSummary summary = svc.runForPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        assertThat(summary.aborted()).isTrue();
        assertThat(summary.abortReason()).contains("Seuil alerte massive franchi");
        verify(emailService).sendAbortAlert(any());
        verify(emailService, never()).sendMonthlyRecap(any());
    }

    @Test
    void runForPeriod_claudeFails_skipsMappingAndContinues() {
        ToolJurisprudenceMapping m1 = buildMapping();
        ToolJurisprudenceMapping m2 = buildMapping();
        when(judilibre.fetchArretsForPeriod(any(), any())).thenReturn(List.of(buildArret("INC1")));
        when(mappingRepo.findAll()).thenReturn(List.of(m1, m2));
        when(evaluator.evaluate(any(), any()))
                .thenThrow(new RuntimeException("anthropic down"))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.CONFIRM, null, new BigDecimal("0.95"), "ok"));

        JurisprudenceWatchService svc = build("AUTO_PILOT", 5);
        JurisprudenceWatchRunSummary summary = svc.runForPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1));

        assertThat(summary.skipped()).isEqualTo(1);
        assertThat(summary.autoConfirm()).isEqualTo(1);
        assertThat(summary.aborted()).isFalse();
    }

    @Test
    void filterCandidates_keepsMatchingJuridiction() {
        JurisprudenceWatchService svc = build("AUTO_PILOT", 5);
        ToolJurisprudenceMapping mapping = buildMapping();
        JudilibreArret matchingSoc = new JudilibreArret("A1", "ref", "Cour de cassation, chambre sociale",
                LocalDate.now(), "n1", "c", "url");
        JudilibreArret otherCiv = new JudilibreArret("A2", "ref", "Cour de cassation, chambre civile",
                LocalDate.now(), "n2", "c", "url");

        List<JudilibreArret> filtered = svc.filterCandidates(mapping, List.of(matchingSoc, otherCiv));

        assertThat(filtered).extracting(JudilibreArret::judilibreId).contains("A1").doesNotContain("A2");
    }

    @Test
    void filterCandidates_emptyMappingJuridiction_keepsAll() {
        JurisprudenceWatchService svc = build("AUTO_PILOT", 5);
        ToolJurisprudenceMapping mapping = buildMapping();
        mapping.setJuridiction("");
        JudilibreArret a1 = new JudilibreArret("A1", "ref", "anything", LocalDate.now(), "n", "c", "url");

        List<JudilibreArret> filtered = svc.filterCandidates(mapping, List.of(a1));

        assertThat(filtered).hasSize(1);
    }

    @Test
    void filterCandidates_capsAt20() {
        JurisprudenceWatchService svc = build("AUTO_PILOT", 5);
        ToolJurisprudenceMapping mapping = buildMapping();
        mapping.setJuridiction("");
        List<JudilibreArret> many = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            many.add(new JudilibreArret("A" + i, "ref", "x", LocalDate.now(), "n", "c", "url"));
        }

        List<JudilibreArret> filtered = svc.filterCandidates(mapping, many);

        assertThat(filtered).hasSize(20);
    }

    private ToolJurisprudenceMapping buildMapping() {
        ToolJurisprudenceMapping m = new ToolJurisprudenceMapping();
        m.setId(UUID.randomUUID());
        m.setToolId("f-dt-30");
        m.setBrancheCalculId("branche");
        m.setArretRef("ref");
        m.setJuridiction("Cour de cassation, chambre sociale");
        m.setDateArret(LocalDate.of(2024, 3, 12));
        m.setNumeroPourvoi("22-XXX");
        m.setLienLegifrance("url");
        m.setChapeauOfficiel("chapeau");
        m.setConfidenceScore(new BigDecimal("0.90"));
        return m;
    }

    private JudilibreArret buildArret(String id) {
        return new JudilibreArret(id, "ref " + id, "Cour de cassation, chambre sociale",
                LocalDate.of(2025, 1, 8), "23-12.345", "chapeau " + id, "url");
    }
}
