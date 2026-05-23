package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-01 — F-DT-38 rupture période d'essai. */
@Component
public class RupturePeriodeEssaiToolUsageContributor implements ToolUsageContributor {

    private final RupturePeriodeEssaiRepository repository;

    public RupturePeriodeEssaiToolUsageContributor(RupturePeriodeEssaiRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RupturePeriodeEssaiToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        RupturePeriodeEssaiToolBranchRegistry.TOOL_ID,
                        RupturePeriodeEssaiToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
