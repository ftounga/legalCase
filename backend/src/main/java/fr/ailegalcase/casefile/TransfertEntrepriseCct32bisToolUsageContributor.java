package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-08 — détecte l'usage de l'outil
 * {@code transfert-entreprise-cct-32bis} (BELGIQUE) via la présence
 * d'une {@link TransfertEntrepriseCct32bisAnalysis} pour le
 * {@code caseFileId} considéré. Permet au mapping jurisprudence de
 * remonter les arrêts pertinents (Cass. BE, Cour du travail BE, CJUE
 * Süzen / Abler sur l'identité économique, art. 7 CCT 32bis sur le
 * maintien des contrats, art. 3 Directive 2001/23/CE sur la solidarité)
 * dans la synthèse de dossier.
 */
@Component
public class TransfertEntrepriseCct32bisToolUsageContributor
        implements ToolUsageContributor {

    private final TransfertEntrepriseCct32bisAnalysisRepository repository;

    public TransfertEntrepriseCct32bisToolUsageContributor(
            TransfertEntrepriseCct32bisAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return TransfertEntrepriseCct32bisToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        TransfertEntrepriseCct32bisToolBranchRegistry.TOOL_ID,
                        TransfertEntrepriseCct32bisToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
