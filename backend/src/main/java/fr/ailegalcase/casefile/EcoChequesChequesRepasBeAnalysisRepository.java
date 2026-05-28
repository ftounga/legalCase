package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-21 : accès JPA aux {@link EcoChequesChequesRepasBeAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface EcoChequesChequesRepasBeAnalysisRepository
        extends JpaRepository<EcoChequesChequesRepasBeAnalysis, UUID> {

    Optional<EcoChequesChequesRepasBeAnalysis> findByCaseFileId(
            UUID caseFileId);
}
