package fr.ailegalcase.casefile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * F-180 SF-180-01 — enregistre en base les crashes de mappers {@link DashboardTile}
 * catchés par {@code CaseFileDashboardService.addSafely()}.
 *
 * <p>Deux contraintes structurantes :</p>
 * <ul>
 *   <li><strong>{@code Propagation.REQUIRES_NEW}</strong> : la persistance du crash
 *       committe dans une transaction indépendante de l'assemblage des tiles
 *       (en lecture seule). La méthode transactionnelle vit dans le bean séparé
 *       {@link CrashPersister} pour passer par le proxy Spring (une
 *       auto-invocation court-circuiterait {@code @Transactional}).</li>
 *   <li><strong>fail-open du fail-open</strong> : si l'INSERT échoue lui-même
 *       (DB indisponible), l'exception est catchée — l'instrumentation ne doit
 *       JAMAIS dégrader le dashboard de l'avocat (invariant 3 du cadrage de
 *       cohérence F-180).</li>
 * </ul>
 */
@Component
public class DashboardTileCrashRecorder {

    private static final Logger log = LoggerFactory.getLogger(DashboardTileCrashRecorder.class);

    private final CrashPersister crashPersister;

    public DashboardTileCrashRecorder(CrashPersister crashPersister) {
        this.crashPersister = crashPersister;
    }

    /**
     * Persiste un crash de mapper. N'est jamais propagée : toute exception de
     * persistance est catchée et loggée WARN.
     *
     * @param toolId     identifiant du mapper concerné (littéral de {@code new DashboardTile})
     * @param caseFileId dossier en cours d'assemblage — nullable
     * @param exception  exception jetée par le mapper
     */
    public void record(String toolId, UUID caseFileId, Exception exception) {
        try {
            crashPersister.persist(toolId, caseFileId, exception);
        } catch (Exception persistenceFailure) {
            // Fail-open du fail-open : l'instrumentation ne dégrade jamais le dashboard.
            log.warn("F-180 — échec de persistance d'un crash de tile (toolId={}): {}",
                    toolId, persistenceFailure.toString());
        }
    }

    /**
     * Bean dédié portant la transaction {@code REQUIRES_NEW} — appelé via le
     * proxy Spring depuis {@link DashboardTileCrashRecorder}.
     */
    @Component
    static class CrashPersister {

        private final DashboardTileCrashRepository crashRepository;

        CrashPersister(DashboardTileCrashRepository crashRepository) {
            this.crashRepository = crashRepository;
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void persist(String toolId, UUID caseFileId, Exception exception) {
            DashboardTileCrash crash = new DashboardTileCrash();
            crash.setToolId(toolId);
            crash.setCaseFileId(caseFileId);
            crash.setExceptionClass(exception.getClass().getName());
            crash.setExceptionMessage(truncate(exception.getMessage()));
            crashRepository.save(crash);
        }

        private static String truncate(String message) {
            if (message == null) {
                return null;
            }
            return message.length() <= DashboardTileCrash.MAX_MESSAGE_LENGTH
                    ? message
                    : message.substring(0, DashboardTileCrash.MAX_MESSAGE_LENGTH);
        }
    }
}
