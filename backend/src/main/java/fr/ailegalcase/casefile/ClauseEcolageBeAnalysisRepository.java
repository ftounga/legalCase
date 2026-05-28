package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-17 : accès JPA aux {@link ClauseEcolageBeAnalysis} — unicité
 * fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface ClauseEcolageBeAnalysisRepository
        extends JpaRepository<ClauseEcolageBeAnalysis, UUID> {

    Optional<ClauseEcolageBeAnalysis> findByCaseFileId(UUID caseFileId);
}
