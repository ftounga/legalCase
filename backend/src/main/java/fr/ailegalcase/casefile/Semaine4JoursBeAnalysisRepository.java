package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-18 : accès JPA aux {@link Semaine4JoursBeAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface Semaine4JoursBeAnalysisRepository
        extends JpaRepository<Semaine4JoursBeAnalysis, UUID> {

    Optional<Semaine4JoursBeAnalysis> findByCaseFileId(UUID caseFileId);
}
