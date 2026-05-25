package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de contestation-c4-onem BE via ContestationC4OnemAnalysis. */
@Component
public class ContestationC4OnemToolUsageContributor implements ToolUsageContributor {
    private final ContestationC4OnemRepository repository;

    public ContestationC4OnemToolUsageContributor(ContestationC4OnemRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ContestationC4OnemToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        ContestationC4OnemToolBranchRegistry.TOOL_ID,
                        ContestationC4OnemToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
