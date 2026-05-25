package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de tribunal-famille-be-mesures-prov BE via TribunalFamilleBeMesuresProvisoiresAnalysis. */
@Component
public class TribunalFamilleBeMesuresProvisoiresToolUsageContributor implements ToolUsageContributor {
    private final TribunalFamilleBeMesuresProvisoiresRepository repository;

    public TribunalFamilleBeMesuresProvisoiresToolUsageContributor(TribunalFamilleBeMesuresProvisoiresRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return TribunalFamilleBeMesuresProvisoiresToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        TribunalFamilleBeMesuresProvisoiresToolBranchRegistry.TOOL_ID,
                        TribunalFamilleBeMesuresProvisoiresToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
