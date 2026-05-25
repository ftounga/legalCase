package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v4 — détecte l'usage de F-FA-25-majeurs-proteges via MajeursProtegesAnalysis. */
@Component
public class MajeursProtegesToolUsageContributor implements ToolUsageContributor {
    private final MajeursProtegesRepository repository;

    public MajeursProtegesToolUsageContributor(MajeursProtegesRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return MajeursProtegesToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        MajeursProtegesToolBranchRegistry.TOOL_ID,
                        MajeursProtegesToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
