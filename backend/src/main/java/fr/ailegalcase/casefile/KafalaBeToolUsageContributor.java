package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** SF-223-03 — kafala-be-recueil-legal (Famille BELGIQUE). */
@Component
public class KafalaBeToolUsageContributor implements ToolUsageContributor {

    private final KafalaBeRepository repository;

    public KafalaBeToolUsageContributor(KafalaBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return KafalaBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        KafalaBeToolBranchRegistry.TOOL_ID,
                        KafalaBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
