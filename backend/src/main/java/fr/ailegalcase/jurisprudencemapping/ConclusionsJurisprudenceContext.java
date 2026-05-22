package fr.ailegalcase.jurisprudencemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * F-JU-02 / SF-JU-02-01 — service qui assemble, pour un dossier donné, les
 * arrêts mappés (F-JU-01) des outils décisionnels effectivement utilisés.
 *
 * <p>Consommé par {@code CaseConclusionService} lors de l'assemblage du prompt
 * F-98 (section {@code JURISPRUDENCE APPLICABLE PAR OUTIL}). Pattern miroir
 * de {@code PiecesPromptContext} F-92.</p>
 *
 * <p>Déduplication d'arrêt par {@code arret_ref} unique (un même arrêt cité
 * par 2 outils différents n'apparaît qu'une fois dans le prompt).</p>
 */
@Service
public class ConclusionsJurisprudenceContext {

    private static final Logger log = LoggerFactory.getLogger(ConclusionsJurisprudenceContext.class);

    private final ToolUsageAggregator usageAggregator;
    private final ToolJurisprudenceService jurisprudenceService;

    public ConclusionsJurisprudenceContext(ToolUsageAggregator usageAggregator,
                                           ToolJurisprudenceService jurisprudenceService) {
        this.usageAggregator = usageAggregator;
        this.jurisprudenceService = jurisprudenceService;
    }

    /**
     * Retourne la liste agrégée des arrêts mappés par outil utilisé, déjà
     * dédupliquée par {@code arret_ref}.
     */
    public List<ToolJurisprudenceCitationByTool> collectForCaseFile(UUID caseFileId) {
        if (caseFileId == null) {
            return Collections.emptyList();
        }
        List<ToolUsage> usages = usageAggregator.detectAll(caseFileId);
        if (usages.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> seenArretRefs = new HashSet<>();
        List<ToolJurisprudenceCitationByTool> result = new ArrayList<>();

        for (ToolUsage usage : usages) {
            try {
                List<ToolJurisprudenceCitationResponse> citations = jurisprudenceService
                        .findByToolAndBranch(usage.toolId(), usage.brancheCalculId());
                if (citations.isEmpty()) {
                    continue;
                }
                List<ToolJurisprudenceCitationResponse> deduped = new ArrayList<>();
                for (ToolJurisprudenceCitationResponse c : citations) {
                    if (seenArretRefs.add(c.arretRef())) {
                        deduped.add(c);
                    }
                }
                if (!deduped.isEmpty()) {
                    result.add(new ToolJurisprudenceCitationByTool(
                            usage.toolId(), usage.brancheCalculId(), deduped));
                }
            } catch (Exception e) {
                log.warn("F-JU-02 — collectForCaseFile failed for tool {}: {}",
                        usage.toolId(), e.getMessage());
            }
        }

        return result;
    }
}
