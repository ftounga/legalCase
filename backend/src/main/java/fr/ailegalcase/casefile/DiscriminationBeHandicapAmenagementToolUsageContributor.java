package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-23 — détecte l'usage de l'outil
 * {@code discrimination-be-handicap-amenagement} (BELGIQUE) via la
 * présence d'une
 * {@link DiscriminationBeHandicapAmenagementAnalysis} pour le
 * {@code caseFileId} considéré. Permet au mapping jurisprudence de
 * remonter les arrêts pertinents (Cass. BE 09/01/2017 S.15.0035.N
 * discrimination indirecte par refus d'aménagement, Cass. BE
 * 18/12/2017 S.16.0014.F charge disproportionnée stricte, CJUE
 * 11/04/2013 C-335/11 et C-337/11 HK Danmark / Ring et Werge notion
 * fonctionnelle de handicap, CJUE 11/09/2019 C-397/18 Nobel
 * Plastiques obligation de rechercher activement une solution,
 * CJUE 11/07/2006 C-13/05 Chacón Navas, CJUE 17/07/2008 C-303/06
 * Coleman protection par ricochet) dans la synthèse de dossier.
 */
@Component
public class DiscriminationBeHandicapAmenagementToolUsageContributor
        implements ToolUsageContributor {

    private final DiscriminationBeHandicapAmenagementAnalysisRepository repository;

    public DiscriminationBeHandicapAmenagementToolUsageContributor(
            DiscriminationBeHandicapAmenagementAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DiscriminationBeHandicapAmenagementToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        DiscriminationBeHandicapAmenagementToolBranchRegistry.TOOL_ID,
                        DiscriminationBeHandicapAmenagementToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
