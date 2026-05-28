package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-29 - detecte l'usage de l'outil
 * {@code at-mp-rente-capital-be} (BELGIQUE) via la presence d'une
 * {@link AtMpRenteCapitalBeAnalysis} pour le {@code caseFileId}
 * considere. Permet au mapping jurisprudence de remonter les arrets
 * pertinents (Cass. BE 06/11/2000 S.99.0119.F capitalisation d'office
 * sous 19 pour cent, Cass. BE 26/05/2003 S.01.0079.F rente viagere,
 * Cass. BE 23/03/2009 S.08.0072.F delai d'epreuve 3 ans conversion
 * partielle) dans la synthese de dossier.
 */
@Component
public class AtMpRenteCapitalBeToolUsageContributor
        implements ToolUsageContributor {

    private final AtMpRenteCapitalBeAnalysisRepository repository;

    public AtMpRenteCapitalBeToolUsageContributor(
            AtMpRenteCapitalBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AtMpRenteCapitalBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        AtMpRenteCapitalBeToolBranchRegistry.TOOL_ID,
                        AtMpRenteCapitalBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
