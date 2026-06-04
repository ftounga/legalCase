package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** SF-223-09 — etat-civil-be-modification (Famille BELGIQUE). */
@Component
public class EtatCivilBeModificationToolUsageContributor implements ToolUsageContributor {

    private final EtatCivilBeModificationRepository repository;

    public EtatCivilBeModificationToolUsageContributor(EtatCivilBeModificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return EtatCivilBeModificationToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        EtatCivilBeModificationToolBranchRegistry.TOOL_ID,
                        EtatCivilBeModificationToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
