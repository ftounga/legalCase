package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-IM-26-regroupement-10ter-be (F-215 P2 Immigration BE / SF-215-03). */
@Component
public class Regroupement10terBeToolUsageContributor implements ToolUsageContributor {

    private final Regroupement10terBeRepository repository;

    public Regroupement10terBeToolUsageContributor(Regroupement10terBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return Regroupement10terBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        Regroupement10terBeToolBranchRegistry.TOOL_ID,
                        Regroupement10terBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
