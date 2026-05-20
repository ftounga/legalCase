package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-217-11 : repository de l'analyse de dévolution / réserve héréditaire belge
 * (1:1 par dossier).
 */
public interface SuccessionBeDevolutionReserveRepository
        extends JpaRepository<SuccessionBeDevolutionReserveAnalysis, UUID> {

    Optional<SuccessionBeDevolutionReserveAnalysis> findByCaseFileId(UUID caseFileId);
}
