package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.ailegalcase.casefile.DashboardAuditDtos.DashboardAuditReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-180 SF-180-01 — Tests unitaires de {@link DashboardAuditService}.
 */
class DashboardAuditServiceTest {

    private DashboardTileCrashRepository crashRepository;
    private DashboardAuditRunRepository runRepository;
    private JdbcTemplate jdbcTemplate;
    private DashboardAuditService service;

    @BeforeEach
    void setUp() {
        crashRepository = mock(DashboardTileCrashRepository.class);
        runRepository = mock(DashboardAuditRunRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new DashboardAuditService(crashRepository, runRepository, jdbcTemplate, objectMapper);

        // Defaults : aucune table découverte, aucun crash.
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of());
        when(crashRepository.findByOccurredAtAfter(any())).thenReturn(List.of());
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private DashboardTileCrash crash(String toolId, String exClass, String msg, Instant when) {
        DashboardTileCrash c = new DashboardTileCrash();
        c.setToolId(toolId);
        c.setExceptionClass(exClass);
        c.setExceptionMessage(msg);
        c.setOccurredAt(when);
        return c;
    }

    @Test
    void runAudit_groupsCrashesByToolIdOverWindow_andPersistsRun() {
        Instant now = Instant.now();
        when(crashRepository.findByOccurredAtAfter(any())).thenReturn(List.of(
                crash("F-DT-08-licenciement-validity", "java.lang.NullPointerException", "npe1",
                        now.minus(2, ChronoUnit.HOURS)),
                crash("F-DT-08-licenciement-validity", "com.fasterxml.JsonMappingException", "json2",
                        now.minus(1, ChronoUnit.HOURS)),
                crash("F-IM-05-arbre-decisionnel-titre", "java.lang.IllegalStateException", "ise",
                        now.minus(3, ChronoUnit.HOURS))));

        DashboardAuditReport report = service.runAudit();

        assertThat(report.crashedMappers()).hasSize(2);
        // Trié par crashCount desc → licenciement (2) avant immigration (1).
        assertThat(report.crashedMappers().get(0).toolId()).isEqualTo("F-DT-08-licenciement-validity");
        assertThat(report.crashedMappers().get(0).crashCount()).isEqualTo(2);
        // Dernière exception = la plus récente.
        assertThat(report.crashedMappers().get(0).lastExceptionClass())
                .isEqualTo("com.fasterxml.JsonMappingException");
        verify(runRepository).save(any(DashboardAuditRun.class));
    }

    @Test
    void runAudit_purgesCrashesOlderThanRetention() {
        service.runAudit();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(crashRepository).deleteByOccurredAtBefore(cutoff.capture());
        // Le cutoff est ~30 jours dans le passé.
        assertThat(cutoff.getValue())
                .isBefore(Instant.now().minus(29, ChronoUnit.DAYS))
                .isAfter(Instant.now().minus(31, ChronoUnit.DAYS));
    }

    @Test
    void runAudit_emptyWhenNoCrash() {
        DashboardAuditReport report = service.runAudit();
        assertThat(report.crashedMappers()).isEmpty();
        assertThat(report.ranAt()).isNotNull();
    }

    @Test
    void runAudit_splitsTablesIntoDormantAndActiveSortedByCountDesc() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("licenciement_analyses", "regime_algerien_analyses",
                        "anciennete_analyses"));
        when(jdbcTemplate.queryForObject("SELECT count(*) FROM licenciement_analyses", Long.class))
                .thenReturn(142L);
        when(jdbcTemplate.queryForObject("SELECT count(*) FROM regime_algerien_analyses", Long.class))
                .thenReturn(0L);
        when(jdbcTemplate.queryForObject("SELECT count(*) FROM anciennete_analyses", Long.class))
                .thenReturn(7L);

        DashboardAuditReport report = service.runAudit();

        // regime_algerien_analyses dormante ; les 3 tables extra (immigration_*)
        // sont aussi dormantes (count non stubbé → 0).
        assertThat(report.dormantTiles()).extracting("tableName")
                .contains("regime_algerien_analyses");
        // Actives triées par rowCount desc.
        assertThat(report.activeTiles()).extracting("tableName")
                .containsExactly("licenciement_analyses", "anciennete_analyses");
        assertThat(report.activeTiles().get(0).rowCount()).isEqualTo(142L);
    }

    @Test
    void runAudit_excludesPipelineTables() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("case_analyses", "chunk_analyses", "document_analyses",
                        "licenciement_analyses"));
        when(jdbcTemplate.queryForObject("SELECT count(*) FROM licenciement_analyses", Long.class))
                .thenReturn(3L);

        service.runAudit();

        // Les 3 tables pipeline ne sont jamais comptées.
        verify(jdbcTemplate, never()).queryForObject(eq("SELECT count(*) FROM case_analyses"), eq(Long.class));
        verify(jdbcTemplate, never()).queryForObject(eq("SELECT count(*) FROM chunk_analyses"), eq(Long.class));
        verify(jdbcTemplate, never()).queryForObject(eq("SELECT count(*) FROM document_analyses"), eq(Long.class));
    }

    @Test
    void runAudit_ignoresTableWhoseCountThrows() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("licenciement_analyses", "broken_analyses"));
        when(jdbcTemplate.queryForObject("SELECT count(*) FROM licenciement_analyses", Long.class))
                .thenReturn(5L);
        when(jdbcTemplate.queryForObject("SELECT count(*) FROM broken_analyses", Long.class))
                .thenThrow(new RuntimeException("table dropped"));

        DashboardAuditReport report = service.runAudit();

        // La table en erreur est ignorée du rapport, pas d'exception propagée.
        assertThat(report.activeTiles()).extracting("tableName")
                .containsExactly("licenciement_analyses");
        assertThat(report.dormantTiles()).extracting("tableName")
                .doesNotContain("broken_analyses");
        assertThat(report.activeTiles()).extracting("tableName")
                .doesNotContain("broken_analyses");
    }

    @Test
    void discoverDecisionalTables_alwaysIncludesExtraNonSuffixTables() {
        List<String> tables = service.discoverDecisionalTables();
        assertThat(tables).contains(
                "immigration_recours", "immigration_title_decisions", "immigration_work_rights");
    }

    @Test
    void getLatest_triggersRunWhenNoRunExists() {
        when(runRepository.findFirstByOrderByRanAtDesc()).thenReturn(Optional.empty());

        DashboardAuditReport report = service.getLatest();

        assertThat(report).isNotNull();
        // getLatest a déclenché un runAudit → une row persistée.
        verify(runRepository, atLeastOnce()).save(any(DashboardAuditRun.class));
    }

    @Test
    void getLatest_deserializesExistingRun() throws Exception {
        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
        DashboardAuditRun existing = new DashboardAuditRun();
        existing.setId(UUID.randomUUID());
        existing.setRanAt(Instant.parse("2026-05-19T08:00:00Z"));
        existing.setCrashedJson("[]");
        existing.setDormantJson("[]");
        existing.setActiveJson(om.writeValueAsString(List.of(
                new DashboardAuditDtos.TileTableCount("licenciement_analyses", 9L))));
        when(runRepository.findFirstByOrderByRanAtDesc()).thenReturn(Optional.of(existing));

        DashboardAuditReport report = service.getLatest();

        assertThat(report.ranAt()).isEqualTo(Instant.parse("2026-05-19T08:00:00Z"));
        assertThat(report.activeTiles()).hasSize(1);
        // Aucun nouveau run déclenché — on a lu l'existant.
        verify(runRepository, never()).save(any(DashboardAuditRun.class));
    }
}
