package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v4 — détecte l'usage de F-FA-21-separation-corps via SeparationCorpsAnalysis. */
@Component
public class SeparationCorpsToolUsageContributor implements ToolUsageContributor {
    private final SeparationCorpsRepository repository;

    public SeparationCorpsToolUsageContributor(SeparationCorpsRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return SeparationCorpsToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        SeparationCorpsToolBranchRegistry.TOOL_ID,
                        SeparationCorpsToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
