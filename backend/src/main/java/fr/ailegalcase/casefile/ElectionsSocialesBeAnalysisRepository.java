package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-09 : accès JPA aux {@link ElectionsSocialesBeAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface ElectionsSocialesBeAnalysisRepository
        extends JpaRepository<ElectionsSocialesBeAnalysis, UUID> {

    Optional<ElectionsSocialesBeAnalysis> findByCaseFileId(UUID caseFileId);
}
