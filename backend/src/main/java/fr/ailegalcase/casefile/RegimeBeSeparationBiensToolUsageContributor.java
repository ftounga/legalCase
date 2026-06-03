package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** SF-223-06 — regime-be-separation-biens (Famille BELGIQUE). */
@Component
public class RegimeBeSeparationBiensToolUsageContributor implements ToolUsageContributor {

    private final RegimeBeSeparationBiensRepository repository;

    public RegimeBeSeparationBiensToolUsageContributor(RegimeBeSeparationBiensRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RegimeBeSeparationBiensToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        RegimeBeSeparationBiensToolBranchRegistry.TOOL_ID,
                        RegimeBeSeparationBiensToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
