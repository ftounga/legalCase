package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-IM-29-naturalisation-conjoint-belge-be (F-215 P2 Immigration BE / SF-215-09). */
@Component
public class NaturalisationConjointBelgeBeToolUsageContributor implements ToolUsageContributor {

    private final NaturalisationConjointBelgeBeRepository repository;

    public NaturalisationConjointBelgeBeToolUsageContributor(NaturalisationConjointBelgeBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return NaturalisationConjointBelgeBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        NaturalisationConjointBelgeBeToolBranchRegistry.TOOL_ID,
                        NaturalisationConjointBelgeBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
