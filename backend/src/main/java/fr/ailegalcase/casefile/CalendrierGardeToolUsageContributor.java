package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-05 — F-FA-06-calendrier-garde. */
@Component
public class CalendrierGardeToolUsageContributor implements ToolUsageContributor {

    private final CalendrierGardeRepository repository;

    public CalendrierGardeToolUsageContributor(CalendrierGardeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return CalendrierGardeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        CalendrierGardeToolBranchRegistry.TOOL_ID,
                        CalendrierGardeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
