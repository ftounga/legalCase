package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** SF-223-08 — dip-be-reconnaissance-decision-etrangere (Famille BELGIQUE). */
@Component
public class DipBeReconnaissanceDecisionEtrangereToolUsageContributor implements ToolUsageContributor {

    private final DipBeReconnaissanceDecisionEtrangereRepository repository;

    public DipBeReconnaissanceDecisionEtrangereToolUsageContributor(
            DipBeReconnaissanceDecisionEtrangereRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DipBeReconnaissanceDecisionEtrangereToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        DipBeReconnaissanceDecisionEtrangereToolBranchRegistry.TOOL_ID,
                        DipBeReconnaissanceDecisionEtrangereToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
