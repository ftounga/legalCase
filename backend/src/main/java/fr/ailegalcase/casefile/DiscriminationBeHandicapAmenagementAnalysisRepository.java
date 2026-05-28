package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-23 : accès JPA aux
 * {@link DiscriminationBeHandicapAmenagementAnalysis} — unicité
 * fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface DiscriminationBeHandicapAmenagementAnalysisRepository
        extends JpaRepository<DiscriminationBeHandicapAmenagementAnalysis, UUID> {

    Optional<DiscriminationBeHandicapAmenagementAnalysis> findByCaseFileId(UUID caseFileId);
}
