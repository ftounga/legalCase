package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-05 — F-FA-02-pension-alimentaire. */
@Component
public class PensionAlimentaireEnfantFrToolUsageContributor implements ToolUsageContributor {

    private final PensionAlimentaireEnfantFrRepository repository;

    public PensionAlimentaireEnfantFrToolUsageContributor(PensionAlimentaireEnfantFrRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return PensionAlimentaireEnfantFrToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        PensionAlimentaireEnfantFrToolBranchRegistry.TOOL_ID,
                        PensionAlimentaireEnfantFrToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
