package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e — détecte l'usage de F-FA-AUDITION-MINEUR via AuditionMineurAnalysis. */
@Component
public class AuditionMineurToolUsageContributor implements ToolUsageContributor {
    private final AuditionMineurAnalysisRepository repository;

    public AuditionMineurToolUsageContributor(AuditionMineurAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AuditionMineurToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        AuditionMineurToolBranchRegistry.TOOL_ID,
                        AuditionMineurToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
