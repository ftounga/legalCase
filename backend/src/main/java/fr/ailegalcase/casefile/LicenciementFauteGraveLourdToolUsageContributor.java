package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-01 — F-DT-36 licenciement faute grave/lourde. */
@Component
public class LicenciementFauteGraveLourdToolUsageContributor implements ToolUsageContributor {

    private final LicenciementFauteGraveLourdRepository repository;

    public LicenciementFauteGraveLourdToolUsageContributor(LicenciementFauteGraveLourdRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return LicenciementFauteGraveLourdToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        LicenciementFauteGraveLourdToolBranchRegistry.TOOL_ID,
                        LicenciementFauteGraveLourdToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
