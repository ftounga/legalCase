package fr.ailegalcase.jurisprudencemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * F-JU-01 / SF-JU-01-03 — agrège tous les beans {@link ToolBranchRegistry} du
 * contexte Spring et expose leur union.
 *
 * <p>V1 : aucun outil n'implémente encore {@link ToolBranchRegistry} → le
 * registry agrégé est vide. Le {@link JurisprudenceDriftService} possède un
 * garde-fou « registry vide » qui suspend tout archive massif accidentel.</p>
 */
@Component
public class ToolBranchRegistryAggregator {

    private static final Logger log = LoggerFactory.getLogger(ToolBranchRegistryAggregator.class);

    private final List<ToolBranchRegistry> registries;

    public ToolBranchRegistryAggregator(List<ToolBranchRegistry> registries) {
        this.registries = registries == null ? List.of() : registries;
    }

    /**
     * Union des branches déclarées par les outils du contexte. Jamais
     * {@code null}. Vide si aucun {@link ToolBranchRegistry} n'est enregistré.
     */
    public Set<String> allKnownBranches() {
        if (registries.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> union = new HashSet<>();
        for (ToolBranchRegistry registry : registries) {
            try {
                Set<String> branches = registry.knownBranches();
                if (branches != null) {
                    union.addAll(branches);
                }
            } catch (Exception e) {
                log.warn("F-JU-01 — ToolBranchRegistry {} threw: {}", registry.getClass().getSimpleName(), e.getMessage());
            }
        }
        return union;
    }
}
