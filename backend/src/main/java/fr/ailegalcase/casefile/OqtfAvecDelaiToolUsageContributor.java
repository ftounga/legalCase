package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-03 — F-IM-08-oqtf-avec-delai-fr. */
@Component
public class OqtfAvecDelaiToolUsageContributor implements ToolUsageContributor {

    private final OqtfAvecDelaiRepository repository;

    public OqtfAvecDelaiToolUsageContributor(OqtfAvecDelaiRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return OqtfAvecDelaiToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        OqtfAvecDelaiToolBranchRegistry.TOOL_ID,
                        OqtfAvecDelaiToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
