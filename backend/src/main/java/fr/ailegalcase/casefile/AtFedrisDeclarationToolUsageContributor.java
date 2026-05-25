package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de at-fedris-declaration BE via AtFedrisDeclarationAnalysis. */
@Component
public class AtFedrisDeclarationToolUsageContributor implements ToolUsageContributor {
    private final AtFedrisDeclarationRepository repository;

    public AtFedrisDeclarationToolUsageContributor(AtFedrisDeclarationRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AtFedrisDeclarationToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        AtFedrisDeclarationToolBranchRegistry.TOOL_ID,
                        AtFedrisDeclarationToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
