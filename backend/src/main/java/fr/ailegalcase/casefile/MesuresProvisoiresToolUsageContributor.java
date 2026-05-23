package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-05 — F-FA-12-mesures-provisoires. */
@Component
public class MesuresProvisoiresToolUsageContributor implements ToolUsageContributor {

    private final MesuresProvisoiresRepository repository;

    public MesuresProvisoiresToolUsageContributor(MesuresProvisoiresRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return MesuresProvisoiresToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        MesuresProvisoiresToolBranchRegistry.TOOL_ID,
                        MesuresProvisoiresToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
