package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-05 — F-FA-08-divorce-alteration. */
@Component
public class DivorceAlterationToolUsageContributor implements ToolUsageContributor {

    private final DivorceAlterationRepository repository;

    public DivorceAlterationToolUsageContributor(DivorceAlterationRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DivorceAlterationToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        DivorceAlterationToolBranchRegistry.TOOL_ID,
                        DivorceAlterationToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
