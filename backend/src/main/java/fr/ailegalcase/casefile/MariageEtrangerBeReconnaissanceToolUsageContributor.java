package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-02-04-06 — mariage-etranger-be-reconnaissance. */
@Component
public class MariageEtrangerBeReconnaissanceToolUsageContributor implements ToolUsageContributor {

    private final MariageEtrangerBeReconnaissanceRepository repository;

    public MariageEtrangerBeReconnaissanceToolUsageContributor(MariageEtrangerBeReconnaissanceRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return MariageEtrangerBeReconnaissanceToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        MariageEtrangerBeReconnaissanceToolBranchRegistry.TOOL_ID,
                        MariageEtrangerBeReconnaissanceToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
