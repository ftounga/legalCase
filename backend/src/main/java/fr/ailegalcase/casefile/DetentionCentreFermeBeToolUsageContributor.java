package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-IM-56-detention-centre-ferme-be (F-221 P3 Immigration BE / SF-221-04). */
@Component
public class DetentionCentreFermeBeToolUsageContributor implements ToolUsageContributor {

    private final DetentionCentreFermeBeRepository repository;

    public DetentionCentreFermeBeToolUsageContributor(DetentionCentreFermeBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DetentionCentreFermeBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        DetentionCentreFermeBeToolBranchRegistry.TOOL_ID,
                        DetentionCentreFermeBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
