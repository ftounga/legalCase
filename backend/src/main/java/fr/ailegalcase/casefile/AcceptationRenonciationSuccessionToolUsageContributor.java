package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99d — détecte l'usage de acceptation-renonciation-succession via AcceptationRenonciationSuccessionAnalysis. */
@Component
public class AcceptationRenonciationSuccessionToolUsageContributor implements ToolUsageContributor {
    private final AcceptationRenonciationSuccessionRepository repository;

    public AcceptationRenonciationSuccessionToolUsageContributor(AcceptationRenonciationSuccessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AcceptationRenonciationSuccessionToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        AcceptationRenonciationSuccessionToolBranchRegistry.TOOL_ID,
                        AcceptationRenonciationSuccessionToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
