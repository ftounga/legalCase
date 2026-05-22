package fr.ailegalcase.jurisprudencemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * F-JU-01 / SF-JU-01-03 — orchestre le cron dérive quotidienne.
 *
 * <p>Compare l'union des {@link ToolBranchRegistry} aux mappings actifs.
 * Mappings dont la clé {@code toolId:brancheCalculId} n'est pas dans le
 * registry → archive automatique + audit log {@code AUTO_ARCHIVE}.</p>
 *
 * <p>Garde-fou : si le registry est vide ET il existe ≥ 1 mapping actif →
 * abort run (évite tout archive massif tant que les outils n'ont pas déclaré
 * leurs branches).</p>
 */
@Service
public class JurisprudenceDriftService {

    private static final Logger log = LoggerFactory.getLogger(JurisprudenceDriftService.class);

    private final ToolJurisprudenceMappingRepository mappingRepository;
    private final JurisprudenceAuditLogRepository auditLogRepository;
    private final ToolBranchRegistryAggregator registryAggregator;

    public JurisprudenceDriftService(ToolJurisprudenceMappingRepository mappingRepository,
                                     JurisprudenceAuditLogRepository auditLogRepository,
                                     ToolBranchRegistryAggregator registryAggregator) {
        this.mappingRepository = mappingRepository;
        this.auditLogRepository = auditLogRepository;
        this.registryAggregator = registryAggregator;
    }

    @Transactional
    public JurisprudenceDriftRunSummary runDriftScan() {
        List<ToolJurisprudenceMapping> active = mappingRepository.findAll().stream()
                .filter(m -> !m.isArchived())
                .toList();

        if (active.isEmpty()) {
            log.info("F-JU-01 — JurisprudenceDriftService: 0 mapping actif, no-op");
            return new JurisprudenceDriftRunSummary(0, 0, false, null);
        }

        Set<String> known = registryAggregator.allKnownBranches();
        if (known.isEmpty()) {
            String reason = "ToolBranchRegistry vide, cron dérive abandonne pour éviter archive massive (" + active.size() + " mappings actifs)";
            log.warn("F-JU-01 — JurisprudenceDriftService abort: {}", reason);
            return JurisprudenceDriftRunSummary.aborted(active.size(), reason);
        }

        int archived = 0;
        for (ToolJurisprudenceMapping mapping : active) {
            String key = mapping.getToolId() + ":" + mapping.getBrancheCalculId();
            if (known.contains(key)) {
                continue;
            }
            mapping.setArchived(true);
            mappingRepository.save(mapping);
            JurisprudenceAuditLog entry = new JurisprudenceAuditLog();
            entry.setMapping(mapping);
            entry.setAction(JurisprudenceAuditAction.AUTO_ARCHIVE);
            entry.setActor(JurisprudenceAuditActor.CRON);
            entry.setClaudeReason("Branche orpheline détectée par cron dérive (clé " + key + " absente du ToolBranchRegistry)");
            auditLogRepository.save(entry);
            archived++;
        }

        log.info("F-JU-01 — JurisprudenceDriftService done: {} mappings actifs, {} orphelins archivés",
                active.size(), archived);
        return new JurisprudenceDriftRunSummary(active.size(), archived, false, null);
    }
}
