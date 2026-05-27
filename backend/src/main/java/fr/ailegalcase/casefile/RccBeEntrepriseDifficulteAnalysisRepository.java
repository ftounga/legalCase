package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-03 : accès JPA aux {@link RccBeEntrepriseDifficulteAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface RccBeEntrepriseDifficulteAnalysisRepository
        extends JpaRepository<RccBeEntrepriseDifficulteAnalysis, UUID> {

    Optional<RccBeEntrepriseDifficulteAnalysis> findByCaseFileId(UUID caseFileId);
}
