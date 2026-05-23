package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-03 — F-IM-09-aes-humanitaire. */
@Component
public class AesHumanitaireToolUsageContributor implements ToolUsageContributor {

    private final AesHumanitaireRepository repository;

    public AesHumanitaireToolUsageContributor(AesHumanitaireRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AesHumanitaireToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        AesHumanitaireToolBranchRegistry.TOOL_ID,
                        AesHumanitaireToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
