package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-IM-28-naturalisation-12bis-be (F-215 P2 Immigration BE / SF-215-07). */
@Component
public class Naturalisation12bisBeToolUsageContributor implements ToolUsageContributor {

    private final Naturalisation12bisBeRepository repository;

    public Naturalisation12bisBeToolUsageContributor(Naturalisation12bisBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return Naturalisation12bisBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        Naturalisation12bisBeToolBranchRegistry.TOOL_ID,
                        Naturalisation12bisBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
