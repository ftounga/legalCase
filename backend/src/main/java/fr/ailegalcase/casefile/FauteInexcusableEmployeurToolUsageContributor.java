package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-DT-91 faute inexcusable de l'employeur (FR). */
@Component
public class FauteInexcusableEmployeurToolUsageContributor implements ToolUsageContributor {

    private final FauteInexcusableEmployeurRepository repository;

    public FauteInexcusableEmployeurToolUsageContributor(FauteInexcusableEmployeurRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return FauteInexcusableEmployeurToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        FauteInexcusableEmployeurToolBranchRegistry.TOOL_ID,
                        FauteInexcusableEmployeurToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
