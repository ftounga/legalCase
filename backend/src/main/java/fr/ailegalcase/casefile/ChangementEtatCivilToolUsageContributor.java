package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v4 — détecte l'usage de F-FA-26-changement-etat-civil via ChangementEtatCivilAnalysis. */
@Component
public class ChangementEtatCivilToolUsageContributor implements ToolUsageContributor {
    private final ChangementEtatCivilRepository repository;

    public ChangementEtatCivilToolUsageContributor(ChangementEtatCivilRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ChangementEtatCivilToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        ChangementEtatCivilToolBranchRegistry.TOOL_ID,
                        ChangementEtatCivilToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
