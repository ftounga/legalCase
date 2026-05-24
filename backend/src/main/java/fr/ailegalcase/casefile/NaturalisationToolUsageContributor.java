package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99c v2 — détecte l'usage de F-IM-13-naturalisation via NaturalisationAnalysis. */
@Component
public class NaturalisationToolUsageContributor implements ToolUsageContributor {
    private final NaturalisationRepository repository;

    public NaturalisationToolUsageContributor(NaturalisationRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return NaturalisationToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        NaturalisationToolBranchRegistry.TOOL_ID,
                        NaturalisationToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
