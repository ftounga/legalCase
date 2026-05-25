package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v4 — détecte l'usage de mediation-familiale-pre-saisine via MediationFamilialePreSaisineAnalysis. */
@Component
public class MediationFamilialePreSaisineToolUsageContributor implements ToolUsageContributor {
    private final MediationFamilialePreSaisineRepository repository;

    public MediationFamilialePreSaisineToolUsageContributor(MediationFamilialePreSaisineRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return MediationFamilialePreSaisineToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        MediationFamilialePreSaisineToolBranchRegistry.TOOL_ID,
                        MediationFamilialePreSaisineToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
