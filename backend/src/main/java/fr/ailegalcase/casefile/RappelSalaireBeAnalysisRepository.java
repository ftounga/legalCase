package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-213-02 : accès JPA aux {@link RappelSalaireBeAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface RappelSalaireBeAnalysisRepository
        extends JpaRepository<RappelSalaireBeAnalysis, UUID> {

    Optional<RappelSalaireBeAnalysis> findByCaseFileId(UUID caseFileId);
}
