package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-207-03 : accès à l'analyse de contestation C4 ONEM — 1 ligne par
 * {@code case_file_id} (unicité enforced en base).
 */
public interface ContestationC4OnemRepository
        extends JpaRepository<ContestationC4OnemAnalysis, UUID> {

    Optional<ContestationC4OnemAnalysis> findByCaseFileId(UUID caseFileId);
}
