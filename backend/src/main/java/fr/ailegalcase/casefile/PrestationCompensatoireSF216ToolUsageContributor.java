package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v3 — détecte l'usage de F-FA-01-prestation-compensatoire via PrestationCompensatoireAnalysisSF216. */
@Component
public class PrestationCompensatoireSF216ToolUsageContributor implements ToolUsageContributor {
    private final PrestationCompensatoireRepositorySF216 repository;

    public PrestationCompensatoireSF216ToolUsageContributor(PrestationCompensatoireRepositorySF216 repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return PrestationCompensatoireSF216ToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        PrestationCompensatoireSF216ToolBranchRegistry.TOOL_ID,
                        PrestationCompensatoireSF216ToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
