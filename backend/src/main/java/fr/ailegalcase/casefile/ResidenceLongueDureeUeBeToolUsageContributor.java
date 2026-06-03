package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-IM-55-residence-longue-duree-ue-be (F-221 P3 Immigration BE / SF-221-03). */
@Component
public class ResidenceLongueDureeUeBeToolUsageContributor implements ToolUsageContributor {

    private final ResidenceLongueDureeUeBeRepository repository;

    public ResidenceLongueDureeUeBeToolUsageContributor(ResidenceLongueDureeUeBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ResidenceLongueDureeUeBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        ResidenceLongueDureeUeBeToolBranchRegistry.TOOL_ID,
                        ResidenceLongueDureeUeBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
