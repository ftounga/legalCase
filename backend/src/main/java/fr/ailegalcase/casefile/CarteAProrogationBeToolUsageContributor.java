package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-IM-53-carte-a-prorogation-be (F-221 P3 Immigration BE / SF-221-01). */
@Component
public class CarteAProrogationBeToolUsageContributor implements ToolUsageContributor {

    private final CarteAProrogationBeRepository repository;

    public CarteAProrogationBeToolUsageContributor(CarteAProrogationBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return CarteAProrogationBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        CarteAProrogationBeToolBranchRegistry.TOOL_ID,
                        CarteAProrogationBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
