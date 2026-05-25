package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99d — détecte l'usage de F-FA-24-testament-validite via TestamentValiditeAnalysis. */
@Component
public class TestamentValiditeToolUsageContributor implements ToolUsageContributor {
    private final TestamentValiditeRepository repository;

    public TestamentValiditeToolUsageContributor(TestamentValiditeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return TestamentValiditeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        TestamentValiditeToolBranchRegistry.TOOL_ID,
                        TestamentValiditeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
