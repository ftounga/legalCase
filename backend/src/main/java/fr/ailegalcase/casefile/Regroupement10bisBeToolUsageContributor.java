package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-IM-27-regroupement-10bis-be (F-215 P2 Immigration BE / SF-215-05). */
@Component
public class Regroupement10bisBeToolUsageContributor implements ToolUsageContributor {

    private final Regroupement10bisBeRepository repository;

    public Regroupement10bisBeToolUsageContributor(Regroupement10bisBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return Regroupement10bisBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        Regroupement10bisBeToolBranchRegistry.TOOL_ID,
                        Regroupement10bisBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
