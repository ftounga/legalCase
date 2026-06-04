package fr.ailegalcase.jurisprudencemapping;

import fr.ailegalcase.auth.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

/**
 * F-JU-06 / SF-JU-06-02 — ré-évaluation et archivage des mappings de
 * jurisprudence <strong>déjà en base</strong>.
 *
 * <p>SF-JU-06-01 a durci le pipeline pour les <em>futurs</em> mappings ; ce
 * service applique les mêmes garde-fous à l'<em>existant</em> et ARCHIVE les
 * mappings non conformes (chapeau vide, confiance &lt; seuil, hors-sujet via la
 * 2ᵉ passe). C'est l'étape qui élimine les citations douteuses déjà stockées
 * (ex. « restauration ferroviaire » sur F-DT-09). Aucune interrogation
 * JUDILIBRE — on ne fait que filtrer l'existant. Le re-remplissage des outils
 * dégarnis est traité par SF-JU-06-03.</p>
 *
 * <p>Déclenché par un SUPER_ADMIN ; exécution asynchrone (≈ 1 appel LLM par
 * mapping conservé jusqu'à la 2ᵉ passe). Doit tourner hors fenêtre de
 * déploiement (un rolling update tue les jobs async).</p>
 */
@Service
public class JurisprudenceReevaluationService {

    private static final Logger log = LoggerFactory.getLogger(JurisprudenceReevaluationService.class);

    /** Seuil de confiance minimal — aligné sur {@code MIN_BOOTSTRAP_CONFIDENCE} (SF-JU-06-01). */
    private static final BigDecimal MIN_CONFIDENCE = new BigDecimal("0.70");

    private final ToolJurisprudenceMappingRepository mappingRepository;
    private final JurisprudenceAuditLogRepository auditLogRepository;
    private final JurisprudenceRelevanceGate relevanceGate;
    private final TransactionTemplate txTemplate;
    private final TaskExecutor taskExecutor;

    public JurisprudenceReevaluationService(ToolJurisprudenceMappingRepository mappingRepository,
                                            JurisprudenceAuditLogRepository auditLogRepository,
                                            JurisprudenceRelevanceGate relevanceGate,
                                            PlatformTransactionManager txManager,
                                            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.mappingRepository = mappingRepository;
        this.auditLogRepository = auditLogRepository;
        this.relevanceGate = relevanceGate;
        this.txTemplate = new TransactionTemplate(txManager);
        this.taskExecutor = taskExecutor;
    }

    /**
     * Lance la ré-évaluation en arrière-plan et retourne le nombre de mappings
     * actifs à évaluer. Les archivages sont consultables dans l'audit log.
     */
    public JurisprudenceReevaluationStarted startReevaluation(User triggerUser) {
        int total = (int) mappingRepository.findByArchivedFalse().size();
        taskExecutor.execute(() -> runReevaluation(triggerUser));
        return new JurisprudenceReevaluationStarted(total);
    }

    /**
     * Exécution synchrone (utilisée par {@link #startReevaluation} via l'executor,
     * et directement par les tests). Retourne le rapport.
     */
    public JurisprudenceReevaluationReport runReevaluation(User triggerUser) {
        List<ToolJurisprudenceMapping> actifs = mappingRepository.findByArchivedFalse();
        int evaluated = 0, archivedEmpty = 0, archivedLowConf = 0, archivedOffTopic = 0, kept = 0;

        for (ToolJurisprudenceMapping mapping : actifs) {
            evaluated++;
            String chapeau = mapping.getChapeauOfficiel();
            if (chapeau == null || chapeau.isBlank()) {
                archive(mapping, "Chapeau officiel vide", triggerUser);
                archivedEmpty++;
                continue;
            }
            if (mapping.getConfidenceScore() == null
                    || mapping.getConfidenceScore().compareTo(MIN_CONFIDENCE) < 0) {
                archive(mapping, "Confiance " + mapping.getConfidenceScore() + " < " + MIN_CONFIDENCE, triggerUser);
                archivedLowConf++;
                continue;
            }
            JurisprudenceRelevanceGate.RelevanceVerdict verdict = relevanceGate.assess(
                    subjectOf(mapping), mapping.getArretRef(), mapping.getJuridiction(), chapeau);
            if (!verdict.pertinent()) {
                archive(mapping, "Hors-sujet : " + verdict.raison(), triggerUser);
                archivedOffTopic++;
                continue;
            }
            kept++;
        }

        JurisprudenceReevaluationReport report = new JurisprudenceReevaluationReport(
                evaluated, archivedEmpty, archivedLowConf, archivedOffTopic, kept);
        log.info("F-JU-06 — Ré-évaluation terminée : {}", report);
        return report;
    }

    private void archive(ToolJurisprudenceMapping mapping, String raison, User triggerUser) {
        txTemplate.executeWithoutResult(status -> {
            mapping.setArchived(true);
            mappingRepository.save(mapping);

            JurisprudenceAuditLog logEntry = new JurisprudenceAuditLog();
            logEntry.setMapping(mapping);
            logEntry.setAction(JurisprudenceAuditAction.AUTO_ARCHIVE);
            logEntry.setActor(JurisprudenceAuditActor.SUPER_ADMIN);
            logEntry.setActorUser(triggerUser);
            logEntry.setClaudeConfidence(mapping.getConfidenceScore());
            logEntry.setClaudeReason("SF-JU-06-02 ré-évaluation : " + raison);
            auditLogRepository.save(logEntry);
        });
        log.info("F-JU-06 — Mapping archivé {}:{} ref={} — {}",
                mapping.getToolId(), mapping.getBrancheCalculId(), mapping.getArretRef(), raison);
    }

    /**
     * Sujet métier dérivé du {@code toolId} (+ branche si spécifique) pour la 2ᵉ
     * passe. Ex. {@code F-DT-09-comparateur-indemnites} → « comparateur indemnites ».
     */
    String subjectOf(ToolJurisprudenceMapping mapping) {
        String toolId = mapping.getToolId() == null ? "" : mapping.getToolId();
        String subject = toolId.replaceAll("(?i)^f-[a-z]+-\\d+-", "").replace('-', ' ').trim();
        if (subject.isBlank()) {
            subject = toolId.replace('-', ' ').trim();
        }
        String branche = mapping.getBrancheCalculId();
        if (branche != null && !branche.isBlank() && !"default".equalsIgnoreCase(branche)) {
            subject = subject + " — " + branche.replace('-', ' ').trim();
        }
        return subject;
    }

    /** Retour immédiat du déclenchement. */
    public record JurisprudenceReevaluationStarted(int totalAEvaluer) {
    }

    /** Rapport de mesure de la ré-évaluation. */
    public record JurisprudenceReevaluationReport(int evaluated, int archivedEmptyChapeau,
                                                  int archivedLowConfidence, int archivedOffTopic, int kept) {
    }
}
