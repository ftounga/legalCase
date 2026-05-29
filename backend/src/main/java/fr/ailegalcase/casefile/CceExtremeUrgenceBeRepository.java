package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** SF-215-15 : repository de l'analyse du recours en extrême urgence CCE (BE). */
public interface CceExtremeUrgenceBeRepository
        extends JpaRepository<CceExtremeUrgenceBeAnalysis, UUID> {

    Optional<CceExtremeUrgenceBeAnalysis> findByCaseFileId(UUID caseFileId);
}
