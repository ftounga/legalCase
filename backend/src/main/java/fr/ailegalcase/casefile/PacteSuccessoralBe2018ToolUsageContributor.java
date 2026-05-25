package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de pacte-successoral-be-2018 BE via PacteSuccessoralBe2018Analysis. */
@Component
public class PacteSuccessoralBe2018ToolUsageContributor implements ToolUsageContributor {
    private final PacteSuccessoralBe2018Repository repository;

    public PacteSuccessoralBe2018ToolUsageContributor(PacteSuccessoralBe2018Repository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return PacteSuccessoralBe2018ToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        PacteSuccessoralBe2018ToolBranchRegistry.TOOL_ID,
                        PacteSuccessoralBe2018ToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
