package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-01 — F-DT-17 indemnité précarité CDD. */
@Component
public class IndemnitePrecariteCddToolUsageContributor implements ToolUsageContributor {

    private final IndemnitePrecariteCddRepository repository;

    public IndemnitePrecariteCddToolUsageContributor(IndemnitePrecariteCddRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return IndemnitePrecariteCddToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        IndemnitePrecariteCddToolBranchRegistry.TOOL_ID,
                        IndemnitePrecariteCddToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
