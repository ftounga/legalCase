package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-207-01 : accès à l'analyse de prescription Travail BE — 1 ligne par
 * {@code case_file_id} (unicité enforced en base).
 */
public interface PrescriptionBeLitigeTravailRepository
        extends JpaRepository<PrescriptionBeLitigeTravailAnalysis, UUID> {

    Optional<PrescriptionBeLitigeTravailAnalysis> findByCaseFileId(UUID caseFileId);
}
