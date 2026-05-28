package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-13 — détecte l'usage de l'outil
 * {@code etudiant-jobiste-be} (BELGIQUE) via la présence d'une
 * {@link EtudiantJobisteBeAnalysis} pour le {@code caseFileId} considéré.
 * Permet au mapping jurisprudence de remonter les arrêts pertinents
 * (jurisprudence ONSS / Cass. BE en matière de requalification de
 * contrat étudiant — vide pédagogique, dépassement de quota,
 * formalisme défaillant) dans la synthèse de dossier.
 */
@Component
public class EtudiantJobisteBeToolUsageContributor
        implements ToolUsageContributor {

    private final EtudiantJobisteBeAnalysisRepository repository;

    public EtudiantJobisteBeToolUsageContributor(
            EtudiantJobisteBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return EtudiantJobisteBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        EtudiantJobisteBeToolBranchRegistry.TOOL_ID,
                        EtudiantJobisteBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
