package fr.ailegalcase.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * F-185 SF-185-06 — recovery automatique des analyses zombies au démarrage du backend.
 *
 * <p>Cause du problème : quand un pod backend redémarre (deploy, OOM, kill), les
 * `CaseAnalysis` qui étaient en `PENDING/PROCESSING/PARTIAL` au moment du kill
 * restent dans cet état pour toujours en DB. Conséquence : l'utilisateur ne peut
 * plus relancer une analyse pour ce dossier, le check serveur retourne HTTP 409
 * "Une analyse est déjà en cours" indéfiniment.
 *
 * <p>Précédent : F-147 SF-147-01 a résolu le même problème pour `DocumentAnalysis`
 * mais au niveau worker (Anthropic exception → mark FAILED). Ce hook au démarrage
 * couvre le cas restart de pod (où le worker n'a pas le temps de logger une exception).
 *
 * <p>Hors scope : `DOCUMENT_ANALYSIS` jobs (déjà géré F-147), recovery basé sur
 * lease/heartbeat (overkill V1).
 *
 * <p>Grace window 600s (10 min) — F-226 SF-226-01 (2026-05-07) : élargie de 30s
 * à 600s pour empêcher le HPA scale-up de tuer des analyses encore actives sur
 * un autre pod. Cas réel staging Immigration Chen 16 (2026-05-07) : enriched
 * analysis 224s OK mais barre rouge UI car HPA a scaled-up à mi-parcours et le
 * nouveau pod a marqué FAILED l'analyse en cours sur le pod d'origine. Les
 * analyses lourdes (3-5 min) sont désormais protégées. Trade-off accepté : une
 * analyse réellement zombie reste PROCESSING jusqu'à 10 min après création (vs
 * 30s avant), mais 99% des analyses légitimes ne sont plus killées à tort. Les
 * timeouts streaming Anthropic (~5 min) couvrent la majorité des cas réels de
 * crash silencieux. Pattern heartbeat différé en V2.
 */
@Component
@Profile({"local", "prod"})
public class PipelineRecoveryRunner {

    private static final Logger log = LoggerFactory.getLogger(PipelineRecoveryRunner.class);

    private static final String STALE_CASE_ANALYSES_SQL = """
            UPDATE case_analyses
            SET analysis_status = 'FAILED',
                updated_at = NOW()
            WHERE analysis_status IN ('PENDING', 'PROCESSING', 'PARTIAL')
              AND created_at < NOW() - (? * INTERVAL '1 second')
            """;

    private static final String STALE_JOBS_SQL = """
            UPDATE analysis_jobs
            SET status = 'FAILED',
                error_message = 'Pipeline interrompu (redémarrage serveur)',
                updated_at = NOW()
            WHERE status IN ('PENDING', 'PROCESSING', 'PARTIAL')
              AND job_type IN ('CASE_ANALYSIS', 'QUESTION_GENERATION', 'ENRICHED_ANALYSIS')
              AND created_at < NOW() - (? * INTERVAL '1 second')
            """;

    private final JdbcTemplate jdbcTemplate;
    private final int graceSeconds;

    public PipelineRecoveryRunner(JdbcTemplate jdbcTemplate,
                                  @Value("${pipeline.recovery.grace-seconds:600}") int graceSeconds) {
        this.jdbcTemplate = jdbcTemplate;
        this.graceSeconds = graceSeconds;
    }

    /**
     * Déclenché une seule fois après que le contexte Spring soit complètement prêt.
     * {@link ApplicationReadyEvent} (vs {@code ContextRefreshedEvent} qui peut se
     * déclencher plusieurs fois sur certains rafraîchissements internes).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        runRecovery();
    }

    /**
     * Méthode publique séparée pour testabilité (appelable directement depuis un test
     * sans avoir à publier un ApplicationReadyEvent).
     */
    @Transactional
    public RecoveryResult runRecovery() {
        log.info("Pipeline recovery starting — grace window {}s", graceSeconds);

        int caseAnalysesMarked = jdbcTemplate.update(STALE_CASE_ANALYSES_SQL, graceSeconds);
        int jobsMarked = jdbcTemplate.update(STALE_JOBS_SQL, graceSeconds);

        if (caseAnalysesMarked > 0 || jobsMarked > 0) {
            log.info("Pipeline recovery on startup — {} case_analyses + {} jobs marked FAILED",
                    caseAnalysesMarked, jobsMarked);
        } else {
            log.debug("Pipeline recovery on startup — nothing to recover");
        }

        return new RecoveryResult(caseAnalysesMarked, jobsMarked);
    }

    public record RecoveryResult(int caseAnalysesMarked, int jobsMarked) {}
}
