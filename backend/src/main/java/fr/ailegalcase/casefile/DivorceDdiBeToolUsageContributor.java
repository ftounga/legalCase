package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de divorce-ddi-3voies-be BE via DivorceDdiBeAnalysis. */
@Component
public class DivorceDdiBeToolUsageContributor implements ToolUsageContributor {
    private final DivorceDdiBeRepository repository;

    public DivorceDdiBeToolUsageContributor(DivorceDdiBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DivorceDdiBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        DivorceDdiBeToolBranchRegistry.TOOL_ID,
                        DivorceDdiBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
