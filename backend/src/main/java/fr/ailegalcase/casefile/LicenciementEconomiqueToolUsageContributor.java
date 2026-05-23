package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-01 — F-DT-13 licenciement économique. */
@Component
public class LicenciementEconomiqueToolUsageContributor implements ToolUsageContributor {

    private final LicenciementEconomiqueRepository repository;

    public LicenciementEconomiqueToolUsageContributor(LicenciementEconomiqueRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return LicenciementEconomiqueToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        LicenciementEconomiqueToolBranchRegistry.TOOL_ID,
                        LicenciementEconomiqueToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
