package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-207-02 : accès à l'analyse C4 ONEM — 1 ligne par {@code case_file_id}
 * (unicité enforced en base).
 */
public interface C4OnemChecklistRepository
        extends JpaRepository<C4OnemChecklistAnalysis, UUID> {

    Optional<C4OnemChecklistAnalysis> findByCaseFileId(UUID caseFileId);
}
