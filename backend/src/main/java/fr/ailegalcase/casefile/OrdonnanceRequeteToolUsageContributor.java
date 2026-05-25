package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v4 — détecte l'usage de F-FA-23-ordonnance-requete via OrdonnanceRequeteAnalysis. */
@Component
public class OrdonnanceRequeteToolUsageContributor implements ToolUsageContributor {
    private final OrdonnanceRequeteRepository repository;

    public OrdonnanceRequeteToolUsageContributor(OrdonnanceRequeteRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return OrdonnanceRequeteToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        OrdonnanceRequeteToolBranchRegistry.TOOL_ID,
                        OrdonnanceRequeteToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
