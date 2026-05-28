package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-26 — détecte l'usage de l'outil
 * {@code travail-noir-be-dimona} (BELGIQUE) via la présence d'une
 * {@link TravailNoirBeDimonaAnalysis} pour le {@code caseFileId}
 * considéré. Permet au mapping jurisprudence de remonter les arrêts
 * pertinents (Cass. ch. néerl. 21/11/2017 P.17.0532.N qualification
 * travail non déclaré niveau 4, Cour const. arrêt 102/2009 du
 * 18/06/2009 conformité présomption de salariat, Cass. ch. fr.
 * 06/12/2010 S.10.0067.F présomption simple renversable) dans la
 * synthèse de dossier.
 */
@Component
public class TravailNoirBeDimonaToolUsageContributor
        implements ToolUsageContributor {

    private final TravailNoirBeDimonaAnalysisRepository repository;

    public TravailNoirBeDimonaToolUsageContributor(
            TravailNoirBeDimonaAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return TravailNoirBeDimonaToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        TravailNoirBeDimonaToolBranchRegistry.TOOL_ID,
                        TravailNoirBeDimonaToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
