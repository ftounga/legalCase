package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PipelineRecoveryRunnerTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final PipelineRecoveryRunner runner = new PipelineRecoveryRunner(jdbcTemplate, 30);

    // T-01 : recovery happy path — 2 case_analyses + 1 job marqués FAILED, log INFO produit
    @Test
    void runRecovery_returnsCountOfMarkedRows() {
        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.contains("case_analyses"), eq(30)))
                .thenReturn(2);
        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.contains("analysis_jobs"), eq(30)))
                .thenReturn(1);

        PipelineRecoveryRunner.RecoveryResult result = runner.runRecovery();

        assertThat(result.caseAnalysesMarked()).isEqualTo(2);
        assertThat(result.jobsMarked()).isEqualTo(1);
    }

    // T-02 : idempotence — 2ᵉ appel sans nouvelles analyses bloquées → 0 update
    @Test
    void runRecovery_idempotent_returnsZeroOnSecondCall() {
        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.anyString(), eq(30)))
                .thenReturn(2)   // 1er appel : 2 rows
                .thenReturn(0);  // 2e appel : 0 rows
        runner.runRecovery();

        when(jdbcTemplate.update(org.mockito.ArgumentMatchers.anyString(), eq(30)))
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
        when(jdbcTemplate.update(sqlCaptor.capture(), eq(30))).thenReturn(0);

        runner.runRecovery();

        String jobsSql = sqlCaptor.getAllValues().stream()
                .filter(s -> s.contains("analysis_jobs"))
                .findFirst().orElseThrow();
        assertThat(jobsSql).contains("CASE_ANALYSIS");
        assertThat(jobsSql).contains("QUESTION_GENERATION");
        assertThat(jobsSql).contains("ENRICHED_ANALYSIS");
        assertThat(jobsSql).doesNotContain("DOCUMENT_ANALYSIS");
    }
}
