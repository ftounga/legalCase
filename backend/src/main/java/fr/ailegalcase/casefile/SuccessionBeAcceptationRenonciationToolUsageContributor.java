package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de succession-be-acceptation-renonciation BE via SuccessionBeAcceptationRenonciationAnalysis. */
@Component
public class SuccessionBeAcceptationRenonciationToolUsageContributor implements ToolUsageContributor {
    private final SuccessionBeAcceptationRenonciationRepository repository;

    public SuccessionBeAcceptationRenonciationToolUsageContributor(SuccessionBeAcceptationRenonciationRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return SuccessionBeAcceptationRenonciationToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        SuccessionBeAcceptationRenonciationToolBranchRegistry.TOOL_ID,
                        SuccessionBeAcceptationRenonciationToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
