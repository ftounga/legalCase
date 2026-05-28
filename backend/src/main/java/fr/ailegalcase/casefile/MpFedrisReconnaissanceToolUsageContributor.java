package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-28 — detecte l'usage de l'outil
 * {@code mp-fedris-reconnaissance} (BELGIQUE) via la presence d'une
 * {@link MpFedrisReconnaissanceAnalysis} pour le {@code caseFileId}
 * considere. Permet au mapping jurisprudence de remonter les arrets
 * pertinents (Cass. BE 28/05/2008 S.07.0033.F systeme ouvert causalite
 * directe et determinante, Cass. BE 04/02/2002 S.01.0095.F caractere
 * direct et essentiel, Cass. BE 13/12/1989 Pas. 1990 I 451 presomption
 * art. 32 juridique renversable, Cour const. 51/2008 conformite regime
 * reparation forfaitaire) dans la synthese de dossier.
 */
@Component
public class MpFedrisReconnaissanceToolUsageContributor
        implements ToolUsageContributor {

    private final MpFedrisReconnaissanceAnalysisRepository repository;

    public MpFedrisReconnaissanceToolUsageContributor(
            MpFedrisReconnaissanceAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return MpFedrisReconnaissanceToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        MpFedrisReconnaissanceToolBranchRegistry.TOOL_ID,
                        MpFedrisReconnaissanceToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
