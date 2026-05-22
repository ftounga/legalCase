package fr.ailegalcase.jurisprudencemapping;

import fr.ailegalcase.auth.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * F-JU-01 / SF-JU-01-05 — orchestrateur du bootstrap manuel des mappings
 * de jurisprudence par un super-admin.
 *
 * <p>Pour chaque entrée {@link JurisprudenceBootstrapEntry} : récupère les
 * candidats JUDILIBRE → demande à Claude de sélectionner 1-3 arrêts
 * structurants → INSERT dans {@code tool_jurisprudence_mappings} avec audit
 * log {@code AUTO_ADD} actor {@code SUPER_ADMIN}.</p>
 */
@Service
public class JurisprudenceBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(JurisprudenceBootstrapService.class);

    private final JudilibreApiClient judilibreClient;
    private final ClaudeJurisprudenceEvaluator evaluator;
    private final ToolJurisprudenceMappingRepository mappingRepository;
    private final JurisprudenceAuditLogRepository auditLogRepository;

    public JurisprudenceBootstrapService(JudilibreApiClient judilibreClient,
                                         ClaudeJurisprudenceEvaluator evaluator,
                                         ToolJurisprudenceMappingRepository mappingRepository,
                                         JurisprudenceAuditLogRepository auditLogRepository) {
        this.judilibreClient = judilibreClient;
        this.evaluator = evaluator;
        this.mappingRepository = mappingRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public JurisprudenceBootstrapResponse runBootstrap(JurisprudenceBootstrapRequest request, User triggerUser) {
        long start = System.currentTimeMillis();
        int processed = 0, created = 0, skipped = 0;
        LocalDate now = LocalDate.now();

        for (JurisprudenceBootstrapEntry entry : request.entries()) {
            processed++;
            LocalDate from = entry.dateMin() != null ? entry.dateMin() : now.minusYears(10);
            List<JudilibreArret> candidates;
            try {
                candidates = judilibreClient.fetchArretsForPeriod(from, now);
            } catch (Exception e) {
                log.warn("F-JU-01 — Bootstrap fetchArrets failed for {}:{}: {}",
                        entry.toolId(), entry.brancheCalculId(), e.getMessage());
                skipped++;
                continue;
            }
            if (candidates.isEmpty()) {
                log.info("F-JU-01 — Bootstrap 0 candidats JUDILIBRE pour {}:{}",
                        entry.toolId(), entry.brancheCalculId());
                skipped++;
                continue;
            }
            candidates = filterByJuridiction(candidates, entry.juridictionFiltre());
            if (candidates.size() > 20) {
                candidates = candidates.subList(0, 20);
            }

            ToolJurisprudenceMapping pseudoMapping = pseudoMappingFromEntry(entry);
            ClaudeEvaluation evaluation = evaluator.evaluate(pseudoMapping, candidates);
            if (evaluation.action() == EvaluationAction.NONE || evaluation.arretChoisi() == null) {
                skipped++;
                continue;
            }
            int inserted = persistTopCandidates(entry, candidates, evaluation, triggerUser);
            created += inserted;
        }

        long duration = System.currentTimeMillis() - start;
        log.info("F-JU-01 — Bootstrap done: {} processed, {} created, {} skipped, {} ms",
                processed, created, skipped, duration);
        return new JurisprudenceBootstrapResponse(processed, created, skipped, duration);
    }

    private int persistTopCandidates(JurisprudenceBootstrapEntry entry,
                                     List<JudilibreArret> candidates,
                                     ClaudeEvaluation evaluation,
                                     User triggerUser) {
        // V1 simple : on persiste seulement l'arrêt choisi par Claude (top-1).
        // V2 : Claude pourrait renvoyer un tableau d'arrêts ; tronquer à 3.
        JudilibreArret chosen = evaluation.arretChoisi();
        ToolJurisprudenceMapping mapping = new ToolJurisprudenceMapping();
        mapping.setToolId(entry.toolId());
        mapping.setBrancheCalculId(entry.brancheCalculId());
        mapping.setArretRef(chosen.ref());
        mapping.setJuridiction(chosen.juridiction() == null ? "Cour de cassation" : chosen.juridiction());
        mapping.setDateArret(chosen.dateArret() == null ? LocalDate.now() : chosen.dateArret());
        mapping.setNumeroPourvoi(chosen.numeroPourvoi() == null ? "n/a" : chosen.numeroPourvoi());
        mapping.setLienLegifrance(chosen.lienLegifrance() == null ? "" : chosen.lienLegifrance());
        mapping.setChapeauOfficiel(chosen.chapeauOfficiel() == null ? "" : chosen.chapeauOfficiel());
        mapping.setLastVerifiedAt(Instant.now());
        mapping.setConfidenceScore(evaluation.confidenceScore() == null ? BigDecimal.ZERO : evaluation.confidenceScore());
        mapping.setArchived(false);
        mappingRepository.save(mapping);

        JurisprudenceAuditLog logEntry = new JurisprudenceAuditLog();
        logEntry.setMapping(mapping);
        logEntry.setAction(JurisprudenceAuditAction.AUTO_ADD);
        logEntry.setActor(JurisprudenceAuditActor.SUPER_ADMIN);
        logEntry.setActorUser(triggerUser);
        logEntry.setClaudeConfidence(evaluation.confidenceScore());
        logEntry.setClaudeReason("Bootstrap initial: " + evaluation.raison());
        auditLogRepository.save(logEntry);
        return 1;
    }

    private List<JudilibreArret> filterByJuridiction(List<JudilibreArret> candidates, String filtre) {
        if (filtre == null || filtre.isBlank()) {
            return candidates;
        }
        String f = filtre.toLowerCase();
        List<JudilibreArret> filtered = new ArrayList<>();
        for (JudilibreArret arret : candidates) {
            String j = arret.juridiction() == null ? "" : arret.juridiction().toLowerCase();
            if (j.contains(f)) {
                filtered.add(arret);
            }
        }
        return filtered.isEmpty() ? candidates : filtered;
    }

    private ToolJurisprudenceMapping pseudoMappingFromEntry(JurisprudenceBootstrapEntry entry) {
        ToolJurisprudenceMapping m = new ToolJurisprudenceMapping();
        m.setToolId(entry.toolId());
        m.setBrancheCalculId(entry.brancheCalculId());
        m.setArretRef("(bootstrap initial — pas de mapping actuel)");
        m.setJuridiction(entry.juridictionFiltre() == null ? "" : entry.juridictionFiltre());
        m.setDateArret(LocalDate.now());
        m.setNumeroPourvoi("");
        m.setLienLegifrance("");
        m.setChapeauOfficiel("Recherche : " + entry.motCleRecherche());
        m.setConfidenceScore(BigDecimal.ZERO);
        return m;
    }
}
