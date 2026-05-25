package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v4 — détecte l'usage de F-FA-13-revisions-post-divorce via RevisionsPostDivorceAnalysis. */
@Component
public class RevisionsPostDivorceToolUsageContributor implements ToolUsageContributor {
    private final RevisionsPostDivorceRepository repository;

    public RevisionsPostDivorceToolUsageContributor(RevisionsPostDivorceRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RevisionsPostDivorceToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        RevisionsPostDivorceToolBranchRegistry.TOOL_ID,
                        RevisionsPostDivorceToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
