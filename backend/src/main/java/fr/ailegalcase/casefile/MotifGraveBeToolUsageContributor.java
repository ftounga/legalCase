package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de F-DT-27-motif-grave-be BE via MotifGraveBeAnalysis. */
@Component
public class MotifGraveBeToolUsageContributor implements ToolUsageContributor {
    private final MotifGraveBeRepository repository;

    public MotifGraveBeToolUsageContributor(MotifGraveBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return MotifGraveBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        MotifGraveBeToolBranchRegistry.TOOL_ID,
                        MotifGraveBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
