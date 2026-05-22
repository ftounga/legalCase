package fr.ailegalcase.jurisprudencemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-02 / SF-JU-02-01 — agrège les beans {@link ToolUsageContributor} du
 * contexte Spring et expose l'usage déclaré pour un dossier.
 *
 * <p>V1 : aucun contributor → liste vide. Comportement neutre.</p>
 */
@Component
public class ToolUsageAggregator {

    private static final Logger log = LoggerFactory.getLogger(ToolUsageAggregator.class);

    private final List<ToolUsageContributor> contributors;

    public ToolUsageAggregator(List<ToolUsageContributor> contributors) {
        this.contributors = contributors == null ? List.of() : contributors;
    }

    public List<ToolUsage> detectAll(UUID caseFileId) {
        if (contributors.isEmpty() || caseFileId == null) {
            return Collections.emptyList();
        }
        List<ToolUsage> usages = new ArrayList<>();
        for (ToolUsageContributor contributor : contributors) {
            try {
                Optional<ToolUsage> usage = contributor.detectUsage(caseFileId);
                usage.ifPresent(usages::add);
            } catch (Exception e) {
                log.warn("F-JU-02 — ToolUsageContributor {} threw on caseFile {}: {}",
                        contributor.getClass().getSimpleName(), caseFileId, e.getMessage());
            }
        }
        return usages;
    }
}
