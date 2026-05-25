package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v4 — détecte l'usage de F-FA-14-ordonnance-protection via OrdonnanceProtectionAnalysis. */
@Component
public class OrdonnanceProtectionToolUsageContributor implements ToolUsageContributor {
    private final OrdonnanceProtectionRepository repository;

    public OrdonnanceProtectionToolUsageContributor(OrdonnanceProtectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return OrdonnanceProtectionToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        OrdonnanceProtectionToolBranchRegistry.TOOL_ID,
                        OrdonnanceProtectionToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
