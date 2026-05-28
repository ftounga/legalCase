package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-19 : accès JPA aux {@link DroitDeconnexionBeAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface DroitDeconnexionBeAnalysisRepository
        extends JpaRepository<DroitDeconnexionBeAnalysis, UUID> {

    Optional<DroitDeconnexionBeAnalysis> findByCaseFileId(UUID caseFileId);
}
