package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-DT-48 mise à pied disciplinaire — régularité (FR). */
@Component
public class MiseAPiedDisciplinaireToolUsageContributor implements ToolUsageContributor {

    private final MiseAPiedDisciplinaireRepository repository;

    public MiseAPiedDisciplinaireToolUsageContributor(MiseAPiedDisciplinaireRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return MiseAPiedDisciplinaireToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        MiseAPiedDisciplinaireToolBranchRegistry.TOOL_ID,
                        MiseAPiedDisciplinaireToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
