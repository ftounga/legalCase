package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-213-01 : accès JPA aux {@link ClauseNonConcurrenceBeAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface ClauseNonConcurrenceBeAnalysisRepository
        extends JpaRepository<ClauseNonConcurrenceBeAnalysis, UUID> {

    Optional<ClauseNonConcurrenceBeAnalysis> findByCaseFileId(UUID caseFileId);
}
