package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil outplacement-be-general-30sem
 * (BELGIQUE) via la présence d'une
 * {@link OutplacementBeGeneral30semAnalysis} pour le {@code caseFileId}
 * considéré. Permet à la mapping jurisprudence de remonter les arrêts
 * pertinents (Cour du travail BE, Cassation, ONEM sur l'outplacement
 * général 30 semaines, sanctions employeur / travailleur, délais d'offre)
 * dans la synthèse de dossier.
 */
@Component
public class OutplacementBeGeneral30semToolUsageContributor
        implements ToolUsageContributor {

    private final OutplacementBeGeneral30semAnalysisRepository repository;

    public OutplacementBeGeneral30semToolUsageContributor(
            OutplacementBeGeneral30semAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return OutplacementBeGeneral30semToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        OutplacementBeGeneral30semToolBranchRegistry.TOOL_ID,
                        OutplacementBeGeneral30semToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
