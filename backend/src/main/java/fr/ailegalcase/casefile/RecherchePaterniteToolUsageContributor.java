package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e — détecte l'usage de F-FA-18-recherche-paternite via RecherchePaterniteAnalysis. */
@Component
public class RecherchePaterniteToolUsageContributor implements ToolUsageContributor {
    private final RecherchePaterniteRepository repository;

    public RecherchePaterniteToolUsageContributor(RecherchePaterniteRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RecherchePaterniteToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        RecherchePaterniteToolBranchRegistry.TOOL_ID,
                        RecherchePaterniteToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
