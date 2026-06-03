package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-IM-54-carte-b-sejour-illimite-be (F-221 P3 Immigration BE / SF-221-02). */
@Component
public class CarteBSejourIllimiteBeToolUsageContributor implements ToolUsageContributor {

    private final CarteBSejourIllimiteBeRepository repository;

    public CarteBSejourIllimiteBeToolUsageContributor(CarteBSejourIllimiteBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return CarteBSejourIllimiteBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        CarteBSejourIllimiteBeToolBranchRegistry.TOOL_ID,
                        CarteBSejourIllimiteBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
