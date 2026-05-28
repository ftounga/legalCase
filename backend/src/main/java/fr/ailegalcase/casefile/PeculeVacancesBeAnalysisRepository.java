package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-20 : accès JPA aux {@link PeculeVacancesBeAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface PeculeVacancesBeAnalysisRepository
        extends JpaRepository<PeculeVacancesBeAnalysis, UUID> {

    Optional<PeculeVacancesBeAnalysis> findByCaseFileId(UUID caseFileId);
}
