package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-02-04-06 — F-IM-14-9ter-medical-be. */
@Component
public class Belgian9terToolUsageContributor implements ToolUsageContributor {

    private final Belgian9terRepository repository;

    public Belgian9terToolUsageContributor(Belgian9terRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return Belgian9terToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        Belgian9terToolBranchRegistry.TOOL_ID,
                        Belgian9terToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
