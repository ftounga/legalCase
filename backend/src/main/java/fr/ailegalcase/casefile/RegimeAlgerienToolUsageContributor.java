package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99c v2 — détecte l'usage de F-IM-17-regime-algerien via RegimeAlgerienAnalysis. */
@Component
public class RegimeAlgerienToolUsageContributor implements ToolUsageContributor {
    private final RegimeAlgerienRepository repository;

    public RegimeAlgerienToolUsageContributor(RegimeAlgerienRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RegimeAlgerienToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        RegimeAlgerienToolBranchRegistry.TOOL_ID,
                        RegimeAlgerienToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
