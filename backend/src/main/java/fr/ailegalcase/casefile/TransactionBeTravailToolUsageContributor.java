package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil transaction-be-travail (BELGIQUE)
 * via la présence d'une {@link TransactionBeTravailAnalysis} pour le
 * {@code caseFileId} considéré. Permet à la mapping jurisprudence de
 * remonter les arrêts pertinents (Cour du travail BE et Cassation sur
 * la portée des clauses de non-recours, l'exigence de concessions
 * réciproques et la nullité des renonciations à des droits d'ordre
 * public) dans la synthèse de dossier.
 */
@Component
public class TransactionBeTravailToolUsageContributor implements ToolUsageContributor {

    private final TransactionBeTravailAnalysisRepository repository;

    public TransactionBeTravailToolUsageContributor(
            TransactionBeTravailAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return TransactionBeTravailToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        TransactionBeTravailToolBranchRegistry.TOOL_ID,
                        TransactionBeTravailToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
