package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-11 : accès JPA aux {@link CongeEducationPayeRegionAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface CongeEducationPayeRegionAnalysisRepository
        extends JpaRepository<CongeEducationPayeRegionAnalysis, UUID> {

    Optional<CongeEducationPayeRegionAnalysis> findByCaseFileId(UUID caseFileId);
}
