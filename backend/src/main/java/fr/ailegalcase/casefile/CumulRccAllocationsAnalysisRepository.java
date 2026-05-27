package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-04 : accès JPA aux {@link CumulRccAllocationsAnalysis} — unicité
 * fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface CumulRccAllocationsAnalysisRepository
        extends JpaRepository<CumulRccAllocationsAnalysis, UUID> {

    Optional<CumulRccAllocationsAnalysis> findByCaseFileId(UUID caseFileId);
}
