package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-DT-49 temps partiel — requalification en temps plein (FR). */
@Component
public class TempsPartielRequalificationToolUsageContributor implements ToolUsageContributor {

    private final TempsPartielRequalificationRepository repository;

    public TempsPartielRequalificationToolUsageContributor(TempsPartielRequalificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return TempsPartielRequalificationToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        TempsPartielRequalificationToolBranchRegistry.TOOL_ID,
                        TempsPartielRequalificationToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
