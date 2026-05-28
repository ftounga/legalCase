package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-21 — détecte l'usage de l'outil
 * {@code eco-cheques-cheques-repas-be} (BELGIQUE) via la présence
 * d'une {@link EcoChequesChequesRepasBeAnalysis} pour le
 * {@code caseFileId} considéré. Permet au mapping jurisprudence de
 * remonter les arrêts pertinents (jurisprudence Cour de cassation
 * Cass. 16/10/2017 S.16.0042.F sur la substitution à rémunération,
 * contentieux ONSS sur le respect des conditions cumulatives
 * art. 19bis AR 28/11/1969, jurisprudence tribunaux du travail sur
 * la qualification des jours effectivement prestés) dans la synthèse
 * de dossier.
 */
@Component
public class EcoChequesChequesRepasBeToolUsageContributor
        implements ToolUsageContributor {

    private final EcoChequesChequesRepasBeAnalysisRepository repository;

    public EcoChequesChequesRepasBeToolUsageContributor(
            EcoChequesChequesRepasBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return EcoChequesChequesRepasBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        EcoChequesChequesRepasBeToolBranchRegistry.TOOL_ID,
                        EcoChequesChequesRepasBeToolBranchRegistry
                                .BRANCHE_DEFAULT));
    }
}
