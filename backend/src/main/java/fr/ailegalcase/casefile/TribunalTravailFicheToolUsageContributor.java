package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de F-DT-06-requete-tribunal-travail BE via TribunalTravailFiche. */
@Component
public class TribunalTravailFicheToolUsageContributor implements ToolUsageContributor {
    private final TribunalTravailFicheRepository repository;

    public TribunalTravailFicheToolUsageContributor(TribunalTravailFicheRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return TribunalTravailFicheToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        TribunalTravailFicheToolBranchRegistry.TOOL_ID,
                        TribunalTravailFicheToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
