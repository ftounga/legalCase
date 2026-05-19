package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.DashboardAuditDtos.CrashedMapper;
import fr.ailegalcase.casefile.DashboardAuditDtos.DashboardAuditReport;
import fr.ailegalcase.casefile.DashboardAuditDtos.TileTableCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * F-180 SF-180-01 — assemble le rapport d'audit des tiles dashboard F-167.
 *
 * <p>Trois panels :</p>
 * <ul>
 *   <li>🔴 <strong>mappers en erreur</strong> — {@code dashboard_tile_crashes}
 *       groupés par {@code toolId} sur les 168 dernières heures ;</li>
 *   <li>🟡 <strong>tiles dormantes</strong> — tables de résultat décisionnel
 *       (suffixe {@code _analyses}) à 0 row ;</li>
 *   <li>🟢 <strong>tiles actives</strong> — mêmes tables à ≥ 1 row, triées
 *       par count décroissant.</li>
 * </ul>
 *
 * <p>Choix de découverte des tables : énumération via {@code INFORMATION_SCHEMA}
 * (SQL-standard — H2 et PostgreSQL) plutôt qu'un map {@code toolId → table} codé
 * en dur (85 entrées, dérive garantie au prochain outil) ou un couplage aux 85
 * repositories. Les 3 tables pipeline ({@code chunk_analyses},
 * {@code document_analyses}, {@code case_analyses}) sont exclues — ce ne sont pas
 * des résultats d'outil décisionnel. Trois tables décisionnelles ne suivant pas
 * le suffixe sont rattrapées explicitement.</p>
 *
 * <p>Complément <strong>runtime</strong> du garde-fou <strong>statique</strong>
 * {@code DashboardTileToolIdIntegrityIT} (SF-DT-36-03).</p>
 */
@Service
public class DashboardAuditService {

    private static final Logger log = LoggerFactory.getLogger(DashboardAuditService.class);

    /** Fenêtre d'agrégation des crashes du panel 🔴. */
    static final int CRASH_WINDOW_HOURS = 168;

    /** Rétention de la table {@code dashboard_tile_crashes}. */
    static final int CRASH_RETENTION_DAYS = 30;

    /**
     * Tables {@code *_analyses} qui NE SONT PAS des résultats d'outil décisionnel
     * (niveaux du pipeline IA chunk → document → dossier). Exclues du comptage.
     */
    private static final Set<String> PIPELINE_ANALYSES_TABLES = Set.of(
            "chunk_analyses",
            "document_analyses",
            "case_analyses"
    );

    /**
     * Tables de résultat décisionnel ne suivant pas le suffixe {@code _analyses}
     * — rattrapées explicitement pour la couverture complète.
     */
    private static final Set<String> EXTRA_DECISIONAL_TABLES = Set.of(
            "immigration_recours",
            "immigration_title_decisions",
            "immigration_work_rights"
    );

    private final DashboardTileCrashRepository crashRepository;
    private final DashboardAuditRunRepository runRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DashboardAuditService(DashboardTileCrashRepository crashRepository,
                                 DashboardAuditRunRepository runRepository,
                                 JdbcTemplate jdbcTemplate,
                                 ObjectMapper objectMapper) {
        this.crashRepository = crashRepository;
        this.runRepository = runRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Exécute un audit complet : agrège les crashes, purge la rétention, compte
     * les rows des tables décisionnelles, persiste une row {@code dashboard_audit_runs}
     * et retourne le rapport.
     */
    @Transactional
    public DashboardAuditReport runAudit() {
        Instant ranAt = Instant.now();

        List<CrashedMapper> crashed = aggregateCrashes(ranAt);
        purgeOldCrashes(ranAt);

        List<TileTableCount> counts = countDecisionalTables();
        List<TileTableCount> dormant = counts.stream()
                .filter(c -> c.rowCount() == 0)
                .sorted(Comparator.comparing(TileTableCount::tableName))
                .collect(Collectors.toList());
        List<TileTableCount> active = counts.stream()
                .filter(c -> c.rowCount() > 0)
                .sorted(Comparator.comparingLong(TileTableCount::rowCount).reversed())
                .collect(Collectors.toList());

        persistRun(ranAt, crashed, dormant, active);
        return new DashboardAuditReport(ranAt, crashed, dormant, active);
    }

    /**
     * Retourne le dernier rapport. Si aucun run n'existe encore (avant le premier
     * lundi 8h), en déclenche un — évite un {@code 404} au premier accès.
     */
    @Transactional
    public DashboardAuditReport getLatest() {
        return runRepository.findFirstByOrderByRanAtDesc()
                .map(this::deserialize)
                .orElseGet(this::runAudit);
    }

    // ── Crashes (panel 🔴) ──────────────────────────────────────────────────

    private List<CrashedMapper> aggregateCrashes(Instant ranAt) {
        Instant since = ranAt.minus(CRASH_WINDOW_HOURS, ChronoUnit.HOURS);
        return crashRepository.findByOccurredAtAfter(since).stream()
                .collect(Collectors.groupingBy(DashboardTileCrash::getToolId))
                .entrySet().stream()
                .map(entry -> {
                    DashboardTileCrash last = entry.getValue().stream()
                            .max(Comparator.comparing(DashboardTileCrash::getOccurredAt))
                            .orElseThrow();
                    return new CrashedMapper(
                            entry.getKey(),
                            entry.getValue().size(),
                            last.getExceptionClass(),
                            last.getExceptionMessage(),
                            last.getOccurredAt());
                })
                .sorted(Comparator.comparingLong(CrashedMapper::crashCount).reversed())
                .collect(Collectors.toList());
    }

    private void purgeOldCrashes(Instant ranAt) {
        Instant cutoff = ranAt.minus(CRASH_RETENTION_DAYS, ChronoUnit.DAYS);
        int purged = crashRepository.deleteByOccurredAtBefore(cutoff);
        if (purged > 0) {
            log.info("F-180 — purge rétention : {} crash(es) de tile > {}j supprimé(s)",
                    purged, CRASH_RETENTION_DAYS);
        }
    }

    // ── Comptage des tables décisionnelles (panels 🟡 / 🟢) ──────────────────

    private List<TileTableCount> countDecisionalTables() {
        List<TileTableCount> counts = new ArrayList<>();
        for (String table : discoverDecisionalTables()) {
            try {
                Long rows = jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM " + table, Long.class);
                counts.add(new TileTableCount(table, rows == null ? 0L : rows));
            } catch (Exception e) {
                // Table droppée concurremment / inaccessible : ignorée du rapport.
                log.warn("F-180 — comptage impossible pour la table {} — ignorée : {}",
                        table, e.toString());
            }
        }
        return counts;
    }

    /**
     * Énumère les tables de résultat décisionnel : suffixe {@code _analyses}
     * (hors pipeline) + le set explicite {@link #EXTRA_DECISIONAL_TABLES}.
     */
    List<String> discoverDecisionalTables() {
        List<String> result = new ArrayList<>();
        try {
            List<String> analysesTables = jdbcTemplate.queryForList(
                    "SELECT table_name FROM information_schema.tables "
                            + "WHERE LOWER(table_name) LIKE '%\\_analyses' ESCAPE '\\'",
                    String.class);
            for (String raw : analysesTables) {
                String table = raw.toLowerCase(Locale.ROOT);
                if (!PIPELINE_ANALYSES_TABLES.contains(table)) {
                    result.add(table);
                }
            }
        } catch (Exception e) {
            log.warn("F-180 — énumération information_schema en échec : {}", e.toString());
        }
        for (String extra : EXTRA_DECISIONAL_TABLES) {
            if (!result.contains(extra)) {
                result.add(extra);
            }
        }
        return result;
    }

    // ── Persistance / (dé)sérialisation ─────────────────────────────────────

    private void persistRun(Instant ranAt,
                            List<CrashedMapper> crashed,
                            List<TileTableCount> dormant,
                            List<TileTableCount> active) {
        try {
            DashboardAuditRun run = new DashboardAuditRun();
            run.setRanAt(ranAt);
            run.setCrashedJson(objectMapper.writeValueAsString(crashed));
            run.setDormantJson(objectMapper.writeValueAsString(dormant));
            run.setActiveJson(objectMapper.writeValueAsString(active));
            runRepository.save(run);
        } catch (Exception e) {
            // La sérialisation JSON d'un rapport ne devrait jamais échouer ;
            // si elle échoue, on log mais on retourne quand même le rapport au caller.
            log.error("F-180 — échec de persistance du run d'audit : {}", e.toString());
        }
    }

    private DashboardAuditReport deserialize(DashboardAuditRun run) {
        try {
            List<CrashedMapper> crashed = objectMapper.readValue(
                    run.getCrashedJson(), new TypeReference<>() {});
            List<TileTableCount> dormant = objectMapper.readValue(
                    run.getDormantJson(), new TypeReference<>() {});
            List<TileTableCount> active = objectMapper.readValue(
                    run.getActiveJson(), new TypeReference<>() {});
            return new DashboardAuditReport(run.getRanAt(), crashed, dormant, active);
        } catch (Exception e) {
            // Run corrompu (cas improbable) : on recalcule plutôt que d'exposer une erreur.
            log.warn("F-180 — run d'audit {} corrompu, recalcul : {}", run.getId(), e.toString());
            return runAudit();
        }
    }
}
