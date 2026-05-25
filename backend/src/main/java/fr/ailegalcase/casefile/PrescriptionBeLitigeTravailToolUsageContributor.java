package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de prescription-be-litige-travail BE via PrescriptionBeLitigeTravailAnalysis. */
@Component
public class PrescriptionBeLitigeTravailToolUsageContributor implements ToolUsageContributor {
    private final PrescriptionBeLitigeTravailRepository repository;

    public PrescriptionBeLitigeTravailToolUsageContributor(PrescriptionBeLitigeTravailRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return PrescriptionBeLitigeTravailToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        PrescriptionBeLitigeTravailToolBranchRegistry.TOOL_ID,
                        PrescriptionBeLitigeTravailToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
