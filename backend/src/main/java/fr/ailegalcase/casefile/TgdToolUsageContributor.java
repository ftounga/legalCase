package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** SF-222-02 — F-FA-TGD (téléphone grave danger — éligibilité). */
@Component
public class TgdToolUsageContributor implements ToolUsageContributor {

    private final TgdRepository repository;

    public TgdToolUsageContributor(TgdRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return TgdToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        TgdToolBranchRegistry.TOOL_ID,
                        TgdToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
