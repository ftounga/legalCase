package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-213-07 : accès JPA aux {@link HarcelementBeProcedureFormelleAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface HarcelementBeProcedureFormelleAnalysisRepository
        extends JpaRepository<HarcelementBeProcedureFormelleAnalysis, UUID> {

    Optional<HarcelementBeProcedureFormelleAnalysis> findByCaseFileId(UUID caseFileId);
}
