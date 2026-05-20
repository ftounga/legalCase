package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-207-07 : accès à l'analyse RCC BE indemnité complémentaire — 1 ligne
 * par {@code case_file_id} (unicité enforced en base).
 */
public interface RccBeIndemniteRepository
        extends JpaRepository<RccBeIndemniteAnalysis, UUID> {

    Optional<RccBeIndemniteAnalysis> findByCaseFileId(UUID caseFileId);
}
