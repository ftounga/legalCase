package fr.ailegalcase.analysis;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PipelineRecoveryRunnerTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    // F-226 SF-226-01 : grace window par défaut = 600s (élargie de 30s)
    private final PipelineRecoveryRunner runner = new PipelineRecoveryRunner(jdbcTemplate, 600);

    private Logger logbackLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void attachAppender() {
        logbackLogger = (Logger) LoggerFactory.getLogger(PipelineRecoveryRunner.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logbackLogger.addAppender(listAppender);
    }

    @AfterEach
    void detachAppender() {
        if (logbackLogger != null && listAppender != null) {
            logbackLogger.detachAppender(listAppender);
        }
    }

    // T-01 : recovery happy path — 2 case_analyses + 1 job marqués FAILED, log INFO produit
    @Test
    void runRecovery_returnsCountOfMarkedRows() {
        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.contains("case_analyses"), eq(600)))
                .thenReturn(2);
        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.contains("analysis_jobs"), eq(600)))
                .thenReturn(1);

        PipelineRecoveryRunner.RecoveryResult result = runner.runRecovery();

        assertThat(result.caseAnalysesMarked()).isEqualTo(2);
        assertThat(result.jobsMarked()).isEqualTo(1);
    }

    // T-02 : idempotence — 2ᵉ appel sans nouvelles analyses bloquées → 0 update
    @Test
    void runRecovery_idempotent_returnsZeroOnSecondCall() {
        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.anyString(), eq(600)))
                .thenReturn(2)   // 1er appel : 2 rows
                .thenReturn(0);  // 2e appel : 0 rows
        runner.runRecovery();

        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.anyString(), eq(600)))
                .thenReturn(0);
        PipelineRecoveryRunner.RecoveryResult result2 = runner.runRecovery();

        assertThat(result2.caseAnalysesMarked()).isEqualTo(0);
        assertThat(result2.jobsMarked()).isEqualTo(0);
    }

    // T-03 : grace seconds appliqué correctement (passé en paramètre du SQL)
    @Test
    void runRecovery_passesGraceSecondsToBothQueries() {
        PipelineRecoveryRunner customRunner = new PipelineRecoveryRunner(jdbcTemplate, 60);
        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.anyString(), eq(60)))
                .thenReturn(0);

        customRunner.runRecovery();

        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("case_analyses"), eq(60));
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("analysis_jobs"), eq(60));
        verifyNoMoreInteractions(jdbcTemplate);
    }

    // T-04 : le SQL filtre bien les job_types autorisés (CASE_ANALYSIS, QUESTION_GENERATION,
    // ENRICHED_ANALYSIS) et exclut DOCUMENT_ANALYSIS qui est géré ailleurs (F-147 SF-147-01)
    @Test
    void runRecovery_jobsSqlFiltersOutDocumentAnalysis() {
        org.mockito.ArgumentCaptor<String> sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        when(jdbcTemplate.update(sqlCaptor.capture(), eq(600))).thenReturn(0);

        runner.runRecovery();

        String jobsSql = sqlCaptor.getAllValues().stream()
                .filter(s -> s.contains("analysis_jobs"))
                .findFirst().orElseThrow();
        assertThat(jobsSql).contains("CASE_ANALYSIS");
        assertThat(jobsSql).contains("QUESTION_GENERATION");
        assertThat(jobsSql).contains("ENRICHED_ANALYSIS");
        assertThat(jobsSql).doesNotContain("DOCUMENT_ANALYSIS");
    }

    // T-CA-04 (F-226 SF-226-01) : analyse créée 5 min avant + grace=600s → préservée.
    // Le SQL filtre `created_at < NOW() - 600 seconds`. Une analyse vieille de 300s
    // (5 min) ne match donc pas le WHERE → 0 row marquée FAILED. On simule ce comportement
    // via le mock JdbcTemplate qui retourne 0 quand grace=600s : aucune analyse récente
    // n'est marquée FAILED.
    @Test
    void runRecovery_preservesAnalysisCreated5MinAgoWithGrace600s() {
        PipelineRecoveryRunner runner600 = new PipelineRecoveryRunner(jdbcTemplate, 600);
        // Avec grace=600s, une analyse de 5 min (300s) reste dans la grace window.
        // Le DELETE/UPDATE SQL ne la matche pas → DB renvoie 0.
        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.contains("case_analyses"), eq(600)))
                .thenReturn(0);
        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.contains("analysis_jobs"), eq(600)))
                .thenReturn(0);

        PipelineRecoveryRunner.RecoveryResult result = runner600.runRecovery();

        assertThat(result.caseAnalysesMarked())
                .as("Analyse créée il y a 5 min doit être préservée par grace window 600s")
                .isEqualTo(0);
        assertThat(result.jobsMarked()).isEqualTo(0);
        // Vérification : la grace 600s a bien été passée au SQL
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("case_analyses"), eq(600));
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("analysis_jobs"), eq(600));
    }

    // T-CA-05 (F-226 SF-226-01) : analyse créée 11 min avant + grace=600s → marquée FAILED.
    // 11 min = 660s > 600s grace → l'analyse est hors fenêtre, elle doit être marquée FAILED.
    // On simule ce comportement via le mock JdbcTemplate qui retourne 1 quand grace=600s.
    @Test
    void runRecovery_marksFailedAnalysisCreated11MinAgoWithGrace600s() {
        PipelineRecoveryRunner runner600 = new PipelineRecoveryRunner(jdbcTemplate, 600);
        // Avec grace=600s, une analyse de 11 min (660s) est hors fenêtre.
        // Le UPDATE SQL la matche → DB renvoie 1.
        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.contains("case_analyses"), eq(600)))
                .thenReturn(1);
        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.contains("analysis_jobs"), eq(600)))
                .thenReturn(0);

        PipelineRecoveryRunner.RecoveryResult result = runner600.runRecovery();

        assertThat(result.caseAnalysesMarked())
                .as("Analyse créée il y a 11 min doit être marquée FAILED (hors grace 600s)")
                .isEqualTo(1);
        assertThat(result.jobsMarked()).isEqualTo(0);
    }

    // T-CA-06 (F-226 SF-226-01) : log INFO `Pipeline recovery starting — grace window 600s`
    // émis au début de runRecovery(), pour faciliter le debug si grace est re-modifiée.
    @Test
    void runRecovery_emitsInfoLogWithGraceWindowAtStartup() {
        PipelineRecoveryRunner runner600 = new PipelineRecoveryRunner(jdbcTemplate, 600);
        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.anyString(), eq(600)))
                .thenReturn(0);

        runner600.runRecovery();

        boolean hasStartLog = listAppender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.INFO
                        && e.getFormattedMessage().equals("Pipeline recovery starting — grace window 600s"));
        assertThat(hasStartLog)
                .as("Log INFO 'Pipeline recovery starting — grace window 600s' doit être émis")
                .isTrue();
    }
}
