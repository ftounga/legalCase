package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-01 — F-DT-36 procédure nullité licenciement. */
@Component
public class ProcedureNulliteLicenciementToolUsageContributor implements ToolUsageContributor {

    private final ProcedureNulliteLicenciementRepository repository;

    public ProcedureNulliteLicenciementToolUsageContributor(ProcedureNulliteLicenciementRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ProcedureNulliteLicenciementToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        ProcedureNulliteLicenciementToolBranchRegistry.TOOL_ID,
                        ProcedureNulliteLicenciementToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
