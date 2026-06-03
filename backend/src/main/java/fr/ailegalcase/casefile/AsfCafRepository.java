package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-222-01 : repository JPA pour l'analyse ASF (art. L. 523-1 CSS).
 */
public interface AsfCafRepository extends JpaRepository<AsfCafAnalysis, UUID> {

    Optional<AsfCafAnalysis> findByCaseFileId(UUID caseFileId);
}
