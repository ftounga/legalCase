package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-217-06 : repository de l'estimation de contribution alimentaire des enfants
 * belge (1:1 par dossier).
 */
public interface ContributionAlimentaireEnfantsBeRepository
        extends JpaRepository<ContributionAlimentaireEnfantsBeAnalysis, UUID> {

    Optional<ContributionAlimentaireEnfantsBeAnalysis> findByCaseFileId(UUID caseFileId);
}
