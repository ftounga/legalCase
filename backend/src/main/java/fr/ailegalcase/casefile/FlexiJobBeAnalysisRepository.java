package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-12 : accès JPA aux {@link FlexiJobBeAnalysis} — unicité
 * fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface FlexiJobBeAnalysisRepository
        extends JpaRepository<FlexiJobBeAnalysis, UUID> {

    Optional<FlexiJobBeAnalysis> findByCaseFileId(UUID caseFileId);
}
