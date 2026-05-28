package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-25 : accès JPA aux {@link AuditoratTravailBeAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface AuditoratTravailBeAnalysisRepository
        extends JpaRepository<AuditoratTravailBeAnalysis, UUID> {

    Optional<AuditoratTravailBeAnalysis> findByCaseFileId(UUID caseFileId);
}
