package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-27 — détecte l'usage de l'outil
 * {@code inastri-statut-travailleur-independant} (BELGIQUE) via la
 * présence d'une {@link InastriStatutTravailleurIndependantAnalysis}
 * pour le {@code caseFileId} considéré. Permet au mapping
 * jurisprudence de remonter les arrêts pertinents (Cass. BE
 * 23/12/2002 S.02.0021.F doctrine Bart Buysse, Cass. BE 06/12/2010
 * S.10.0067.F caractère simple de la présomption art. 328, Cour
 * const. arrêt 167/2013 du 19/12/2013 présomption sectorielle
 * art. 337/2, Cour const. arrêt 102/2009 du 18/06/2009 présomption
 * générale art. 328 § 1) dans la synthèse de dossier.
 */
@Component
public class InastriStatutTravailleurIndependantToolUsageContributor
        implements ToolUsageContributor {

    private final InastriStatutTravailleurIndependantAnalysisRepository repository;

    public InastriStatutTravailleurIndependantToolUsageContributor(
            InastriStatutTravailleurIndependantAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return InastriStatutTravailleurIndependantToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        InastriStatutTravailleurIndependantToolBranchRegistry.TOOL_ID,
                        InastriStatutTravailleurIndependantToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
