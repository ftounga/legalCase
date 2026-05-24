package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-DT-56 égalité salariale femmes/hommes (FR). */
@Component
public class EgaliteSalarialeFhToolUsageContributor implements ToolUsageContributor {

    private final EgaliteSalarialeFhRepository repository;

    public EgaliteSalarialeFhToolUsageContributor(EgaliteSalarialeFhRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return EgaliteSalarialeFhToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        EgaliteSalarialeFhToolBranchRegistry.TOOL_ID,
                        EgaliteSalarialeFhToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
