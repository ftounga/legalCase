package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de divorce-dc-be BE via DivorceDcBeAnalysis. */
@Component
public class DivorceDcBeToolUsageContributor implements ToolUsageContributor {
    private final DivorceDcBeRepository repository;

    public DivorceDcBeToolUsageContributor(DivorceDcBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DivorceDcBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        DivorceDcBeToolBranchRegistry.TOOL_ID,
                        DivorceDcBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
