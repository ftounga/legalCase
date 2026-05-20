package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-207-08 : accès à l'analyse outplacement obligatoire 45+ BE — 1 ligne
 * par {@code case_file_id} (unicité enforced en base).
 */
public interface OutplacementBeRepository
        extends JpaRepository<OutplacementBeAnalysis, UUID> {

    Optional<OutplacementBeAnalysis> findByCaseFileId(UUID caseFileId);
}
