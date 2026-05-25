package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de refere-tribunal-travail-be BE via RefereTribunalTravailBeAnalysis. */
@Component
public class RefereTribunalTravailBeToolUsageContributor implements ToolUsageContributor {
    private final RefereTribunalTravailBeRepository repository;

    public RefereTribunalTravailBeToolUsageContributor(RefereTribunalTravailBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RefereTribunalTravailBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        RefereTribunalTravailBeToolBranchRegistry.TOOL_ID,
                        RefereTribunalTravailBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
