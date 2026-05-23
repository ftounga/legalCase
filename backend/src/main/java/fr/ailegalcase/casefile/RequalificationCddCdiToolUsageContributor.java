package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-01 — F-DT-22 requalification CDD en CDI. */
@Component
public class RequalificationCddCdiToolUsageContributor implements ToolUsageContributor {

    private final RequalificationCddCdiRepository repository;

    public RequalificationCddCdiToolUsageContributor(RequalificationCddCdiRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RequalificationCddCdiToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        RequalificationCddCdiToolBranchRegistry.TOOL_ID,
                        RequalificationCddCdiToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
