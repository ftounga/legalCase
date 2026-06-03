package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** SF-222-03 — F-FA-HABILITATION-FAMILIALE (habilitation familiale, art. 494-1 et s. Cciv). */
@Component
public class HabilitationFamilialeToolUsageContributor implements ToolUsageContributor {

    private final HabilitationFamilialeRepository repository;

    public HabilitationFamilialeToolUsageContributor(HabilitationFamilialeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return HabilitationFamilialeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        HabilitationFamilialeToolBranchRegistry.TOOL_ID,
                        HabilitationFamilialeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
