package fr.ailegalcase.jurisprudencemapping;

import fr.ailegalcase.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SF-JU-01-09 — couvre la bascule du {@code @Transactional} global vers une
 * transaction par-entrée via {@link org.springframework.transaction.support.TransactionTemplate}.
 *
 * <p>Stratégie : on mocke {@link PlatformTransactionManager} et on vérifie que
 * (a) {@code getTransaction} est appelé une fois par entrée persistable,
 * (b) un échec sur une entrée ne bloque pas les suivantes.</p>
 */
class JurisprudenceBootstrapServiceTest {

    private JudilibreApiClient judilibre;
    private ClaudeJurisprudenceEvaluator evaluator;
    private ToolJurisprudenceMappingRepository mappingRepo;
    private JurisprudenceAuditLogRepository auditRepo;
    private PlatformTransactionManager txManager;
    private JurisprudenceBootstrapService service;

    @BeforeEach
    void setUp() {
        judilibre = mock(JudilibreApiClient.class);
        evaluator = mock(ClaudeJurisprudenceEvaluator.class);
        mappingRepo = mock(ToolJurisprudenceMappingRepository.class);
        auditRepo = mock(JurisprudenceAuditLogRepository.class);
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        service = new JurisprudenceBootstrapService(judilibre, evaluator, mappingRepo, auditRepo, txManager);
    }

    @Test
    void runBootstrap_twoAddEntries_runsOneTransactionPerEntry() {
        JudilibreArret arret1 = arret("AAA");
        JudilibreArret arret2 = arret("BBB");
        when(judilibre.fetchArretsForPeriod(any(), any()))
                .thenReturn(List.of(arret1, arret2));
        when(evaluator.evaluate(any(), any()))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, arret1, new BigDecimal("0.92"), "ok"))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, arret2, new BigDecimal("0.91"), "ok"));

        JurisprudenceBootstrapRequest req = new JurisprudenceBootstrapRequest(List.of(
                entry("f-dt-30", "branche-1"),
                entry("f-dt-31", "branche-2")
        ));
        JurisprudenceBootstrapResponse resp = service.runBootstrap(req, triggerUser());

        verify(txManager, times(2)).getTransaction(any());
        verify(txManager, times(2)).commit(any());
        verify(txManager, never()).rollback(any());
        verify(mappingRepo, times(2)).save(any());
        verify(auditRepo, times(2)).save(any());
        assertThat(resp.entriesProcessed()).isEqualTo(2);
        assertThat(resp.mappingsCreated()).isEqualTo(2);
        assertThat(resp.entriesSkipped()).isZero();
    }

    @Test
    void runBootstrap_noneAction_doesNotOpenTransaction() {
        when(judilibre.fetchArretsForPeriod(any(), any()))
                .thenReturn(List.of(arret("AAA")));
        when(evaluator.evaluate(any(), any()))
                .thenReturn(ClaudeEvaluation.none("Pas pertinent"));

        JurisprudenceBootstrapRequest req = new JurisprudenceBootstrapRequest(List.of(
                entry("f-dt-30", "branche-1")
        ));
        JurisprudenceBootstrapResponse resp = service.runBootstrap(req, triggerUser());

        verify(txManager, never()).getTransaction(any());
        verify(mappingRepo, never()).save(any());
        assertThat(resp.mappingsCreated()).isZero();
        assertThat(resp.entriesSkipped()).isEqualTo(1);
    }

    @Test
    void runBootstrap_dbFailureOnSecondEntry_continuesOnThird() {
        JudilibreArret arret1 = arret("AAA");
        JudilibreArret arret2 = arret("BBB");
        JudilibreArret arret3 = arret("CCC");
        when(judilibre.fetchArretsForPeriod(any(), any()))
                .thenReturn(List.of(arret1, arret2, arret3));
        when(evaluator.evaluate(any(), any()))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, arret1, new BigDecimal("0.9"), "ok"))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, arret2, new BigDecimal("0.9"), "ok"))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, arret3, new BigDecimal("0.9"), "ok"));

        // 1er save OK ; 2ème save lève → 3ème save OK
        when(mappingRepo.save(any()))
                .thenAnswer(inv -> inv.getArgument(0))
                .thenThrow(new DataIntegrityViolationException("simulated unique constraint"))
                .thenAnswer(inv -> inv.getArgument(0));

        JurisprudenceBootstrapRequest req = new JurisprudenceBootstrapRequest(List.of(
                entry("f-dt-30", "branche-1"),
                entry("f-dt-31", "branche-2"),
                entry("f-dt-32", "branche-3")
        ));
        JurisprudenceBootstrapResponse resp = service.runBootstrap(req, triggerUser());

        verify(txManager, times(3)).getTransaction(any());
        verify(txManager, atLeastOnce()).rollback(any());
        assertThat(resp.entriesProcessed()).isEqualTo(3);
        assertThat(resp.mappingsCreated()).isEqualTo(2);
        assertThat(resp.entriesSkipped()).isEqualTo(1);
    }

    @Test
    void runBootstrap_fetchJudilibreFails_skipsEntryWithoutTransaction() {
        when(judilibre.fetchArretsForPeriod(any(), any()))
                .thenThrow(new RuntimeException("JUDILIBRE down"));

        JurisprudenceBootstrapRequest req = new JurisprudenceBootstrapRequest(List.of(
                entry("f-dt-30", "branche-1")
        ));
        JurisprudenceBootstrapResponse resp = service.runBootstrap(req, triggerUser());

        verify(txManager, never()).getTransaction(any());
        verify(evaluator, never()).evaluate(any(), any());
        assertThat(resp.entriesSkipped()).isEqualTo(1);
        assertThat(resp.mappingsCreated()).isZero();
    }

    private JurisprudenceBootstrapEntry entry(String toolId, String brancheCalculId) {
        return new JurisprudenceBootstrapEntry(toolId, brancheCalculId, "mot-clé test",
                "Cour de cassation", null);
    }

    private JudilibreArret arret(String id) {
        return new JudilibreArret(id, "Cass. soc. " + id, "Cour de cassation, chambre sociale",
                LocalDate.of(2025, 1, 8), "23-12.345", "Chapeau " + id,
                "https://www.legifrance.gouv.fr/juri/id/" + id);
    }

    private User triggerUser() {
        User u = new User();
        u.setEmail("admin@legalcase.fr");
        return u;
    }
}
