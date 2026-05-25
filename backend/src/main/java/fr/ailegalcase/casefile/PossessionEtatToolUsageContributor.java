package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e — détecte l'usage de F-FA-18-possession-etat via PossessionEtatAnalysis. */
@Component
public class PossessionEtatToolUsageContributor implements ToolUsageContributor {
    private final PossessionEtatRepository repository;

    public PossessionEtatToolUsageContributor(PossessionEtatRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return PossessionEtatToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        PossessionEtatToolBranchRegistry.TOOL_ID,
                        PossessionEtatToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
