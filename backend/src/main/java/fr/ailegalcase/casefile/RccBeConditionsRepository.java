package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-207-06 : accès à l'analyse RCC BE — 1 ligne par {@code case_file_id}
 * (unicité enforced en base).
 */
public interface RccBeConditionsRepository
        extends JpaRepository<RccBeConditionsAnalysis, UUID> {

    Optional<RccBeConditionsAnalysis> findByCaseFileId(UUID caseFileId);
}
