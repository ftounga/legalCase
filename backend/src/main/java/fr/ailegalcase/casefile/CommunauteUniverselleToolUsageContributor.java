package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v3 — détecte l'usage de F-FA-16-communaute-universelle via CommunauteUniverselleAnalysis. */
@Component
public class CommunauteUniverselleToolUsageContributor implements ToolUsageContributor {
    private final CommunauteUniverselleRepository repository;

    public CommunauteUniverselleToolUsageContributor(CommunauteUniverselleRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return CommunauteUniverselleToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        CommunauteUniverselleToolBranchRegistry.TOOL_ID,
                        CommunauteUniverselleToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
