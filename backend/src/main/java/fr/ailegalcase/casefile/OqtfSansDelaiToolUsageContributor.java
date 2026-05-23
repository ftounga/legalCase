package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-03 — F-IM-08-oqtf-sans-delai-fr. */
@Component
public class OqtfSansDelaiToolUsageContributor implements ToolUsageContributor {

    private final OqtfSansDelaiRepository repository;

    public OqtfSansDelaiToolUsageContributor(OqtfSansDelaiRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return OqtfSansDelaiToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        OqtfSansDelaiToolBranchRegistry.TOOL_ID,
                        OqtfSansDelaiToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
