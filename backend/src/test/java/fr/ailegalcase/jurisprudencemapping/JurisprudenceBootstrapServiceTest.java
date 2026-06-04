package fr.ailegalcase.jurisprudencemapping;

import fr.ailegalcase.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
    private JurisprudenceBootstrapJobRepository jobRepo;
    private PlatformTransactionManager txManager;
    private JurisprudenceBeWebSearchClient beWebSearchClient;
    private JurisprudenceRelevanceGate relevanceGate;
    private JudilibreQueryEnricher queryEnricher;
    private JurisprudenceBootstrapService service;

    @BeforeEach
    void setUp() {
        judilibre = mock(JudilibreApiClient.class);
        evaluator = mock(ClaudeJurisprudenceEvaluator.class);
        mappingRepo = mock(ToolJurisprudenceMappingRepository.class);
        auditRepo = mock(JurisprudenceAuditLogRepository.class);
        jobRepo = mock(JurisprudenceBootstrapJobRepository.class);
        beWebSearchClient = mock(JurisprudenceBeWebSearchClient.class);
        relevanceGate = mock(JurisprudenceRelevanceGate.class);
        queryEnricher = mock(JudilibreQueryEnricher.class);
        // SF-JU-06-01 — par défaut la 2ᵉ passe juge pertinent (les tests de rejet l'overrident).
        when(relevanceGate.assess(any(), any()))
                .thenReturn(new JurisprudenceRelevanceGate.RelevanceVerdict(true, "pertinent"));
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        // SyncTaskExecutor : exécute le Runnable immédiatement sur le thread courant —
        // simplifie l'assertion sur l'état du job après startBootstrap.
        service = new JurisprudenceBootstrapService(judilibre, beWebSearchClient, evaluator,
                mappingRepo, auditRepo, jobRepo, txManager, new SyncTaskExecutor(), relevanceGate, queryEnricher);
    }

    // --- SF-JU-06-03 — requêtes JUDILIBRE ciblées ---

    @Test
    void runBootstrap_enrichQueriesTrue_passesEnrichedQueryToJudilibre() {
        when(queryEnricher.enrich(any(), any(), any())).thenReturn("requête ciblée enrichie");
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt())).thenReturn(List.of(arret("AAA")));
        when(evaluator.evaluate(any(), any()))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, arret("AAA"), new BigDecimal("0.90"), "ok"));

        service.runBootstrap(new JurisprudenceBootstrapRequest(List.of(entry("f-dt-30", "branche-1")), true), triggerUser());

        org.mockito.ArgumentCaptor<String> q = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(judilibre).fetchArretsByKeyword(q.capture(), any(), any(), anyInt());
        assertThat(q.getValue()).isEqualTo("requête ciblée enrichie");
        verify(queryEnricher).enrich("f-dt-30", "branche-1", "mot-clé test");
    }

    @Test
    void runBootstrap_enrichQueriesFalse_usesOriginalKeyword_andNeverEnriches() {
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt())).thenReturn(List.of(arret("AAA")));
        when(evaluator.evaluate(any(), any()))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, arret("AAA"), new BigDecimal("0.90"), "ok"));

        service.runBootstrap(new JurisprudenceBootstrapRequest(List.of(entry("f-dt-30", "branche-1"))), triggerUser());

        org.mockito.ArgumentCaptor<String> q = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(judilibre).fetchArretsByKeyword(q.capture(), any(), any(), anyInt());
        assertThat(q.getValue()).isEqualTo("mot-clé test");
        verify(queryEnricher, never()).enrich(any(), any(), any());
    }

    // --- SF-JU-06-01 — garde-fous qualité (chapeau vide / confiance / pertinence) ---

    @Test
    void runBootstrap_emptyChapeau_isRejectedWithoutPersist() {
        JudilibreArret sansChapeau = new JudilibreArret("EMPTY", "Cass. soc. EMPTY",
                "Cour de cassation, chambre sociale", LocalDate.of(2025, 1, 8),
                "23-00.001", "", "https://www.legifrance.gouv.fr/juri/id/EMPTY");
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt()))
                .thenReturn(List.of(sansChapeau));
        when(evaluator.evaluate(any(), any()))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, sansChapeau, new BigDecimal("0.90"), "ok"));

        JurisprudenceBootstrapResponse resp = service.runBootstrap(
                new JurisprudenceBootstrapRequest(List.of(entry("f-dt-30", "branche-1"))), triggerUser());

        verify(mappingRepo, never()).save(any());
        verify(relevanceGate, never()).assess(any(), any()); // rejet avant la 2ᵉ passe
        assertThat(resp.mappingsCreated()).isZero();
        assertThat(resp.entriesSkipped()).isEqualTo(1);
    }

    @Test
    void runBootstrap_lowConfidence_isRejectedWithoutPersist() {
        JudilibreArret arret = arret("LOW");
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt()))
                .thenReturn(List.of(arret));
        when(evaluator.evaluate(any(), any()))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, arret, new BigDecimal("0.55"), "marginal"));

        JurisprudenceBootstrapResponse resp = service.runBootstrap(
                new JurisprudenceBootstrapRequest(List.of(entry("f-dt-30", "branche-1"))), triggerUser());

        verify(mappingRepo, never()).save(any());
        assertThat(resp.mappingsCreated()).isZero();
        assertThat(resp.entriesSkipped()).isEqualTo(1);
    }

    @Test
    void runBootstrap_offTopicArret_isRejectedBySecondPass() {
        JudilibreArret horsSujet = arret("OFFTOPIC"); // chapeau plein, confiance OK…
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt()))
                .thenReturn(List.of(horsSujet));
        when(evaluator.evaluate(any(), any()))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, horsSujet, new BigDecimal("0.72"), "marginal"));
        // …mais la 2ᵉ passe le juge hors-sujet (cas « restauration ferroviaire »)
        when(relevanceGate.assess(any(), any()))
                .thenReturn(new JurisprudenceRelevanceGate.RelevanceVerdict(false, "porte sur une convention sectorielle sans rapport"));

        JurisprudenceBootstrapResponse resp = service.runBootstrap(
                new JurisprudenceBootstrapRequest(List.of(entry("f-dt-09-comparateur-indemnites", "default"))), triggerUser());

        verify(relevanceGate).assess(any(), any());
        verify(mappingRepo, never()).save(any());
        assertThat(resp.mappingsCreated()).isZero();
        assertThat(resp.entriesSkipped()).isEqualTo(1);
    }

    // --- SF-JU-04-02 — routage FR vs BE ---

    @org.junit.jupiter.api.Test
    void isBelgianEntry_detectsByToolId() {
        org.assertj.core.api.Assertions.assertThat(
                JurisprudenceBootstrapService.isBelgianEntry(
                        new JurisprudenceBootstrapEntry("clause-non-concurrence-be", "default", "test", null, null)))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(
                JurisprudenceBootstrapService.isBelgianEntry(
                        new JurisprudenceBootstrapEntry("c4-onem-checklist", "default", "test", null, null)))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(
                JurisprudenceBootstrapService.isBelgianEntry(
                        new JurisprudenceBootstrapEntry("at-fedris-declaration", "default", "test", null, null)))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(
                JurisprudenceBootstrapService.isBelgianEntry(
                        new JurisprudenceBootstrapEntry("F-DT-07-anciennete", "default", "test", null, null)))
                .isFalse();
    }

    @org.junit.jupiter.api.Test
    void runBootstrap_belgianEntry_routesToBeWebSearchClient() {
        JudilibreArret beArret = new JudilibreArret("be-1",
                "Cass. BE 12 mars 2024, n° X.YY.0123.F",
                "Cour de cassation belge", LocalDate.of(2024, 3, 12),
                "X.YY.0123.F", "Chapeau BE",
                "https://juportal.be/content/ECLI:BE:CASS:XYZ");
        when(beWebSearchClient.fetchArretsByKeyword(any(), any(), any(), anyInt()))
                .thenReturn(List.of(beArret));
        when(evaluator.evaluate(any(), any()))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, beArret,
                        new BigDecimal("0.85"), "BE structurant"));

        JurisprudenceBootstrapRequest req = new JurisprudenceBootstrapRequest(List.of(
                entry("outplacement-be-obligatoire-45", "default")
        ));
        JurisprudenceBootstrapResponse resp = service.runBootstrap(req, triggerUser());

        // BE web search a été appelé, JUDILIBRE pas
        verify(beWebSearchClient).fetchArretsByKeyword(any(), any(), any(), anyInt());
        verify(judilibre, never()).fetchArretsByKeyword(any(), any(), any(), anyInt());
        org.assertj.core.api.Assertions.assertThat(resp.mappingsCreated()).isEqualTo(1);
    }

    @org.junit.jupiter.api.Test
    void runBootstrap_frenchEntry_routesToJudilibre() {
        JudilibreArret arret = arret("AAA");
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt()))
                .thenReturn(List.of(arret));
        when(evaluator.evaluate(any(), any()))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, arret,
                        new BigDecimal("0.9"), "ok"));

        JurisprudenceBootstrapRequest req = new JurisprudenceBootstrapRequest(List.of(
                entry("F-DT-07-anciennete", "default")
        ));
        service.runBootstrap(req, triggerUser());

        verify(judilibre).fetchArretsByKeyword(any(), any(), any(), anyInt());
        verify(beWebSearchClient, never()).fetchArretsByKeyword(any(), any(), any(), anyInt());
    }

    @Test
    void runBootstrap_twoAddEntries_runsOneTransactionPerEntry() {
        JudilibreArret arret1 = arret("AAA");
        JudilibreArret arret2 = arret("BBB");
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt()))
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
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt()))
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
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt()))
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
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt()))
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

    // --- SF-JU-01-10 — bootstrap async + polling status ---

    @Test
    void startBootstrap_persistsRunningJobAndReturnsJobId() {
        UUID assignedId = UUID.randomUUID();
        when(jobRepo.save(any(JurisprudenceBootstrapJob.class))).thenAnswer(inv -> {
            JurisprudenceBootstrapJob j = inv.getArgument(0);
            if (j.getId() == null) j.setId(assignedId);
            return j;
        });
        when(jobRepo.findById(assignedId))
                .thenAnswer(inv -> {
                    JurisprudenceBootstrapJob j = new JurisprudenceBootstrapJob();
                    j.setId(assignedId);
                    j.setStatus(JurisprudenceBootstrapJobStatus.RUNNING);
                    j.setEntriesTotal(1);
                    return Optional.of(j);
                });
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt())).thenReturn(List.of(arret("AAA")));
        when(evaluator.evaluate(any(), any()))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, arret("AAA"),
                        new BigDecimal("0.9"), "ok"));

        JurisprudenceBootstrapRequest req = new JurisprudenceBootstrapRequest(List.of(
                entry("f-dt-30", "branche-1")
        ));

        JurisprudenceBootstrapJobStarted started = service.startBootstrap(req, triggerUser());

        assertThat(started.jobId()).isEqualTo(assignedId);
        assertThat(started.entriesTotal()).isEqualTo(1);
        assertThat(started.startedAt()).isNotNull();
        // Le SyncTaskExecutor a déjà exécuté le runner ; on doit avoir au moins
        // l'INSERT initial + 1 progress update + 1 DONE update.
        verify(jobRepo, atLeastOnce()).save(any(JurisprudenceBootstrapJob.class));
    }

    @Test
    void startBootstrap_asyncRunner_marksJobDoneOnSuccess() {
        UUID assignedId = UUID.randomUUID();
        JurisprudenceBootstrapJob persistedJob = new JurisprudenceBootstrapJob();
        persistedJob.setId(assignedId);
        persistedJob.setStatus(JurisprudenceBootstrapJobStatus.RUNNING);
        persistedJob.setEntriesTotal(2);

        when(jobRepo.save(any(JurisprudenceBootstrapJob.class))).thenAnswer(inv -> {
            JurisprudenceBootstrapJob j = inv.getArgument(0);
            if (j.getId() == null) j.setId(assignedId);
            return j;
        });
        when(jobRepo.findById(assignedId)).thenReturn(Optional.of(persistedJob));
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt())).thenReturn(List.of(arret("AAA")));
        when(evaluator.evaluate(any(), any()))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, arret("AAA"),
                        new BigDecimal("0.9"), "ok"));

        JurisprudenceBootstrapRequest req = new JurisprudenceBootstrapRequest(List.of(
                entry("f-dt-30", "branche-1"),
                entry("f-dt-31", "branche-2")
        ));

        service.startBootstrap(req, triggerUser());

        assertThat(persistedJob.getStatus()).isEqualTo(JurisprudenceBootstrapJobStatus.DONE);
        assertThat(persistedJob.getEntriesProcessed()).isEqualTo(2);
        assertThat(persistedJob.getMappingsCreated()).isEqualTo(2);
        assertThat(persistedJob.getCompletedAt()).isNotNull();
        assertThat(persistedJob.getDurationMs()).isNotNull();
        assertThat(persistedJob.getErrorMessage()).isNull();
    }

    @Test
    void startBootstrap_asyncRunner_marksJobFailedOnFatalException() {
        UUID assignedId = UUID.randomUUID();
        JurisprudenceBootstrapJob persistedJob = new JurisprudenceBootstrapJob();
        persistedJob.setId(assignedId);
        persistedJob.setStatus(JurisprudenceBootstrapJobStatus.RUNNING);
        persistedJob.setEntriesTotal(1);

        when(jobRepo.save(any(JurisprudenceBootstrapJob.class))).thenAnswer(inv -> {
            JurisprudenceBootstrapJob j = inv.getArgument(0);
            if (j.getId() == null) j.setId(assignedId);
            return j;
        });
        when(jobRepo.findById(assignedId)).thenReturn(Optional.of(persistedJob));
        // L'exception fatale est levée AVANT la boucle (ex : NPE sur txTemplate)
        // via la confirmation que onProgress relève — on simule via evaluator qui
        // lève une RuntimeException non capturée dans la boucle.
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt())).thenReturn(List.of(arret("AAA")));
        when(evaluator.evaluate(any(), any())).thenThrow(new RuntimeException("evaluator boom"));

        JurisprudenceBootstrapRequest req = new JurisprudenceBootstrapRequest(List.of(
                entry("f-dt-30", "branche-1")
        ));

        service.startBootstrap(req, triggerUser());

        assertThat(persistedJob.getStatus()).isEqualTo(JurisprudenceBootstrapJobStatus.FAILED);
        assertThat(persistedJob.getErrorMessage()).isEqualTo("evaluator boom");
        assertThat(persistedJob.getCompletedAt()).isNotNull();
    }

    @Test
    void findJob_unknownId_returnsEmpty() {
        UUID unknown = UUID.randomUUID();
        when(jobRepo.findById(unknown)).thenReturn(Optional.empty());

        Optional<JurisprudenceBootstrapJob> result = service.findJob(unknown);

        assertThat(result).isEmpty();
        verify(jobRepo).findById(eq(unknown));
    }

    // --- SF-JU-01-14 — bootstrap idempotent : skip si mapping déjà présent ---

    @Test
    void runBootstrap_whenMappingAlreadyExists_skipsWithoutOpeningTransaction() {
        JudilibreArret arret = arret("AAA");
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt())).thenReturn(List.of(arret));
        when(evaluator.evaluate(any(), any()))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, arret, new BigDecimal("0.9"), "ok"));
        when(mappingRepo.existsByToolIdAndBrancheCalculIdAndArretRef(
                eq("f-dt-30"), eq("branche-1"), eq(arret.ref())))
                .thenReturn(true);

        JurisprudenceBootstrapRequest req = new JurisprudenceBootstrapRequest(List.of(
                entry("f-dt-30", "branche-1")
        ));
        JurisprudenceBootstrapResponse resp = service.runBootstrap(req, triggerUser());

        verify(txManager, never()).getTransaction(any());
        verify(mappingRepo, never()).save(any());
        verify(auditRepo, never()).save(any());
        assertThat(resp.entriesProcessed()).isEqualTo(1);
        assertThat(resp.mappingsCreated()).isZero();
        assertThat(resp.entriesSkipped()).isEqualTo(1);
    }

    @Test
    void runBootstrap_mixedNewAndExistingEntries_persistsOnlyNew() {
        JudilibreArret arretA = arret("AAA");
        JudilibreArret arretB = arret("BBB");
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt()))
                .thenReturn(List.of(arretA))
                .thenReturn(List.of(arretB));
        when(evaluator.evaluate(any(), any()))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, arretA, new BigDecimal("0.9"), "ok"))
                .thenReturn(new ClaudeEvaluation(EvaluationAction.ADD, arretB, new BigDecimal("0.9"), "ok"));
        when(mappingRepo.existsByToolIdAndBrancheCalculIdAndArretRef(
                eq("f-dt-30"), eq("branche-1"), eq(arretA.ref())))
                .thenReturn(true);
        when(mappingRepo.existsByToolIdAndBrancheCalculIdAndArretRef(
                eq("f-dt-31"), eq("branche-2"), eq(arretB.ref())))
                .thenReturn(false);

        JurisprudenceBootstrapRequest req = new JurisprudenceBootstrapRequest(List.of(
                entry("f-dt-30", "branche-1"),
                entry("f-dt-31", "branche-2")
        ));
        JurisprudenceBootstrapResponse resp = service.runBootstrap(req, triggerUser());

        verify(txManager, times(1)).getTransaction(any());
        verify(mappingRepo, times(1)).save(any());
        assertThat(resp.mappingsCreated()).isEqualTo(1);
        assertThat(resp.entriesSkipped()).isEqualTo(1);
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
