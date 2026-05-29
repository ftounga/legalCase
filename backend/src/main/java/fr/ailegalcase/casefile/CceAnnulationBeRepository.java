package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** SF-215-13 : repository de l'analyse du recours en annulation CCE (BE). */
public interface CceAnnulationBeRepository
        extends JpaRepository<CceAnnulationBeAnalysis, UUID> {

    Optional<CceAnnulationBeAnalysis> findByCaseFileId(UUID caseFileId);
}
